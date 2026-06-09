package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eduagent.agent.AiClient;
import com.eduagent.entity.Resource;
import com.eduagent.entity.StudentProfile;
import com.eduagent.mapper.ResourceMapper;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final AiClient aiClient;
    private final StudentProfileMapper studentProfileMapper;

    @Override
    public Page<Resource> listResources(int page, int pageSize, String keyword, String type, String status) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Resource::getTitle, keyword);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Resource::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Resource::getStatus, status);
        }
        wrapper.orderByDesc(Resource::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Resource getByChapterId(Long chapterId, String type) {
        return findByChapterAndType(chapterId, type);
    }

    @Override
    public List<Resource> listByChapterId(Long chapterId) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resource::getCourseId, String.valueOf(chapterId));
        wrapper.orderByDesc(Resource::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public Resource generateResource(Long chapterId, String chapterName, String topic, String type, String difficulty, Long studentId) {
        // 1. 先查DB是否已有同章节同类型同难度的资源
        Resource existing = findByChapterAndTypeAndDifficulty(chapterId, type, difficulty);
        if (existing != null && existing.getContent() != null && !existing.getContent().isEmpty()) {
            log.info("命中缓存: chapterId={}, type={}, difficulty={}", chapterId, type, difficulty);
            return existing;
        }

        // 2. 读取学生画像
        Map<String, Object> profileMap = loadStudentProfile(studentId);

        // 3. 调用 AI 智能体生成
        log.info("调用AI生成资源: chapter={}, type={}, difficulty={}", chapterName, type, difficulty);
        Map<String, Object> aiResult = aiClient.generateResource(
                String.valueOf(studentId),
                chapterName,
                topic,
                type,
                difficulty,
                profileMap
        );

        String content = (String) aiResult.getOrDefault("content", "");

        // 4. 存入数据库
        Resource resource = new Resource();
        resource.setTitle(chapterName + " - " + getTypeLabel(type));
        resource.setType(type);
        resource.setDifficulty(difficulty);
        resource.setContent(content);
        resource.setCourseId(String.valueOf(chapterId));
        resource.setCourseName(chapterName);
        resource.setStatus("published");
        resource.setViews(0);
        resource.setFavorites(0);
        resource.setRating(0.0);
        resource.setCreateTime(LocalDateTime.now());
        resource.setUpdateTime(LocalDateTime.now());

        resourceMapper.insert(resource);
        log.info("资源已存入DB: id={}, title={}", resource.getId(), resource.getTitle());

        return resource;
    }

    @Override
    @Transactional
    public Resource regenerateResource(Long resourceId, String difficulty, Long studentId) {
        Resource existing = resourceMapper.selectById(resourceId);
        if (existing == null) {
            throw new RuntimeException("资源不存在: id=" + resourceId);
        }

        Map<String, Object> profileMap = loadStudentProfile(studentId);

        Long chapterId;
        try {
            chapterId = Long.parseLong(existing.getCourseId());
        } catch (NumberFormatException e) {
            chapterId = 0L;
        }

        Map<String, Object> aiResult = aiClient.generateResource(
                String.valueOf(studentId),
                existing.getCourseName() != null ? existing.getCourseName() : existing.getTitle(),
                existing.getTitle(),
                existing.getType(),
                difficulty,
                profileMap
        );

        String content = (String) aiResult.getOrDefault("content", "");

        existing.setContent(content);
        existing.setDifficulty(difficulty);
        existing.setUpdateTime(LocalDateTime.now());
        resourceMapper.updateById(existing);

        log.info("资源已重新生成: id={}, difficulty={}", resourceId, difficulty);
        return existing;
    }

    @Override
    public void saveFeedback(Long resourceId, Boolean liked, String difficultyFeedback) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) return;

        if (liked != null && liked) {
            resource.setFavorites((resource.getFavorites() != null ? resource.getFavorites() : 0) + 1);
        }
        resource.setUpdateTime(LocalDateTime.now());
        resourceMapper.updateById(resource);
        log.info("资源反馈已记录: id={}, liked={}, difficultyFeedback={}", resourceId, liked, difficultyFeedback);
    }

    @Override
    public Resource findByChapterAndType(Long chapterId, String type) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resource::getCourseId, String.valueOf(chapterId));
        wrapper.eq(Resource::getType, type);
        wrapper.orderByDesc(Resource::getCreateTime);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    private Resource findByChapterAndTypeAndDifficulty(Long chapterId, String type, String difficulty) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resource::getCourseId, String.valueOf(chapterId));
        wrapper.eq(Resource::getType, type);
        wrapper.eq(Resource::getDifficulty, difficulty);
        wrapper.orderByDesc(Resource::getCreateTime);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    private Map<String, Object> loadStudentProfile(Long studentId) {
        Map<String, Object> profileMap = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
            if (sp != null) {
                profileMap.put("course", sp.getCourse());
                profileMap.put("topic", sp.getTopic());
                profileMap.put("knowledge_base", sp.getKnowledgeBase());
                profileMap.put("weaknesses", sp.getWeaknesses());
                profileMap.put("pace", sp.getPace());
                profileMap.put("resource_preference", sp.getResourcePreference());
                profileMap.put("last_score", sp.getLastScore());
            }
        } catch (Exception e) {
            log.warn("读取学生画像失败: {}", e.getMessage());
        }
        return profileMap;
    }

    private String getTypeLabel(String type) {
        Map<String, String> labels = new HashMap<>();
        labels.put("mindmap", "思维导图");
        labels.put("quiz", "练习题目");
        labels.put("reading", "拓展阅读");
        labels.put("code", "代码案例");
        labels.put("learning_path", "学习路径");
        return labels.getOrDefault(type, type);
    }
}
