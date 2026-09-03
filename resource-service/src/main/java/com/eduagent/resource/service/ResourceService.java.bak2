package com.eduagent.resource.service;

import com.eduagent.resource.client.AiResourceClient;
import com.eduagent.resource.client.LearningProfileClient;
import com.eduagent.resource.config.RabbitConfig;
import com.eduagent.resource.dto.*;
import com.eduagent.resource.mq.ResourceGenerateMessage;
import com.eduagent.resource.vo.ResourceVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiResourceClient aiClient;

    @Autowired
    private LearningProfileClient profileClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ResourceCacheService cacheService;

    // ========== 原有 CRUD（集成缓存） ==========

    public List<ResourceVO> listAll() {
        String sql = "SELECT id, title, type, difficulty, chapter, chapter_id, status, rating, views, favorites, create_time FROM learning_resources";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ResourceVO.class));
    }

    public ResourceVO getById(Long id) {
        // 1. 尝试从缓存获取
        ResourceVO cached = cacheService.getCachedResourceDetail(id);
        if (cached != null) {
            return cached;
        }

        // 2. 查数据库
        String sql = "SELECT id, title, type, difficulty, chapter, chapter_id, status, rating, views, favorites, create_time FROM learning_resources WHERE id = ?";
        List<ResourceVO> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ResourceVO.class), id);
        if (list.isEmpty()) return null;
        ResourceVO vo = list.get(0);

        // 3. 更新 views（异步，不阻塞返回）
        jdbcTemplate.update("UPDATE learning_resources SET views = views + 1 WHERE id = ?", id);

        // 4. 写入缓存（10分钟）
        cacheService.cacheResourceDetail(id, vo);
        return vo;
    }

    public List<ResourceVO> listByChapter(String chapterId) {
        // 1. 尝试从缓存获取（由于需要按类型区分，这里只查全部类型）
        // 注意：这里未指定 type，缓存查找需要按实际类型进行，通常前端会传 type
        // 对于不带类型的查询，我们直接查数据库，不缓存（因为可能包含多种类型）
        String sql = "SELECT id, title, type, difficulty, chapter, chapter_id, status, rating, views, favorites, create_time FROM learning_resources WHERE chapter_id = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ResourceVO.class), chapterId);
    }

    public List<ResourceVO> listByChapterAndType(String chapterId, String type) {
        // 1. 尝试从缓存获取
        List<ResourceVO> cached = cacheService.getCachedChapterResources(chapterId, type);
        if (cached != null) {
            return cached;
        }

        // 2. 查数据库
        String sql = "SELECT id, title, type, difficulty, chapter, chapter_id, status, rating, views, favorites, create_time FROM learning_resources WHERE chapter_id = ? AND type = ?";
        List<ResourceVO> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ResourceVO.class), chapterId, type);

        // 3. 写入缓存（30分钟）
        if (!list.isEmpty()) {
            cacheService.cacheChapterResources(chapterId, type, list);
        }
        return list;
    }

    public void create(ResourceVO resource) {
        String sql = "INSERT INTO learning_resources (user_id, title, type, difficulty, chapter, chapter_id, description, status, views, favorites, rating, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            resource.getUserId() != null ? resource.getUserId() : 1L,
            resource.getTitle(),
            resource.getType(),
            resource.getDifficulty() != null ? resource.getDifficulty() : "medium",
            resource.getChapter(),
            resource.getChapterId(),
            resource.getDescription() != null ? resource.getDescription() : "",
            resource.getStatus() != null ? resource.getStatus() : "published",
            resource.getViews() != null ? resource.getViews() : 0,
            resource.getFavorites() != null ? resource.getFavorites() : 0,
            resource.getRating() != null ? resource.getRating() : 0.0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    public void delete(Long id) {
        ResourceVO vo = getById(id);
        if (vo != null) {
            // 删除缓存
            cacheService.evictDetailCache(id);
            if (vo.getChapterId() != null && vo.getType() != null) {
                cacheService.evictChapterCache(vo.getChapterId(), vo.getType());
            }
        }
        jdbcTemplate.update("DELETE FROM learning_resources WHERE id = ?", id);
    }

    public void favorite(Long id, boolean favorite) {
        int delta = favorite ? 1 : -1;
        jdbcTemplate.update("UPDATE learning_resources SET favorites = GREATEST(0, favorites + ?) WHERE id = ?", delta, id);
        // 更新缓存中的收藏数（简单处理：删除缓存，下次重新加载）
        cacheService.evictDetailCache(id);
    }

    public void feedback(Long id, FeedbackReq req) {
        Long userId = 1L;
        String sql = "INSERT INTO resource_feedback (user_id, resource_id, liked, difficulty_feedback) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, id, req.getLiked() ? 1 : 0, req.getDifficultyFeedback());
    }

    public List<ResourceVO> myFavorites() {
        Long userId = 1L;
        String sql = "SELECT r.id, r.title, r.type, r.difficulty, r.chapter, r.chapter_id, r.status, r.rating, r.views, r.favorites, r.create_time " +
                     "FROM learning_resources r JOIN resource_favorites f ON r.id = f.resource_id WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ResourceVO.class), userId);
    }

    public void updateResourceContent(Long id, String content, String status) {
        String sql = "UPDATE learning_resources SET content = ?, status = ?, update_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, content, status, LocalDateTime.now(), id);
        // 清除缓存
        cacheService.evictDetailCache(id);
        // 清除对应的章节缓存（需要知道 chapterId 和 type，但这里暂时无法获取，可以在消费者中额外传入）
        // 更好的做法是在消费者中调用带缓存清除的方法
    }

    public void updateResourceStatus(Long id, String status, String errorMsg) {
        String sql = "UPDATE learning_resources SET status = ?, error_msg = ?, update_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, errorMsg, LocalDateTime.now(), id);
        // 清除缓存
        cacheService.evictDetailCache(id);
    }

    // 带缓存清除的更新方法（供消费者调用）
    public void updateResourceContentWithCache(Long id, String content, String status, String chapterId, String type) {
        updateResourceContent(id, content, status);
        // 清除章节缓存
        if (chapterId != null && type != null) {
            cacheService.evictChapterCache(chapterId, type);
        }
    }

    public void updateResourceStatusWithCache(Long id, String status, String errorMsg, String chapterId, String type) {
        updateResourceStatus(id, status, errorMsg);
        if (chapterId != null && type != null) {
            cacheService.evictChapterCache(chapterId, type);
        }
    }

    // ========== 异步生成 ==========

    public ResourceVO generateAsync(ResourceGenerateReq req) {
        ResourceVO resource = new ResourceVO();
        resource.setUserId(req.getUserId());
        resource.setTitle(req.getChapter() + " - " + req.getType());
        resource.setType(req.getType());
        resource.setDifficulty(req.getDifficulty());
        resource.setChapter(req.getChapter());
        resource.setChapterId(req.getChapterId());
        resource.setDescription("AI 生成中...");
        resource.setStatus("generating");
        resource.setViews(0);
        resource.setFavorites(0);
        resource.setRating(0.0);
        create(resource);
        // 获取刚插入的自增 ID
        Long newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        resource.setId(newId);

        ResourceGenerateMessage message = new ResourceGenerateMessage(resource.getId(), req.getUserId(), req);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_RESOURCE,
                RabbitConfig.ROUTING_KEY_GENERATE, message);

        return resource;
    }

    public ResourceVO regenerate(Long id, String newDifficulty) {
        ResourceVO existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("资源不存在");
        }
        if ("generating".equals(existing.getStatus())) {
            return existing;
        }

        String difficulty = (newDifficulty != null) ? newDifficulty : existing.getDifficulty();
        String sql = "UPDATE learning_resources SET status = 'generating', content = NULL, error_msg = NULL, difficulty = ?, update_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, difficulty, LocalDateTime.now(), id);
        // 清除详情缓存
        cacheService.evictDetailCache(id);
        // 清除章节缓存
        if (existing.getChapterId() != null && existing.getType() != null) {
            cacheService.evictChapterCache(existing.getChapterId(), existing.getType());
        }

        ResourceVO updated = getById(id);

        ResourceGenerateReq req = new ResourceGenerateReq();
        req.setUserId(updated.getUserId());
        req.setChapter(updated.getChapter());
        req.setChapterName(updated.getChapter());
        String topic = updated.getTitle();
        if (topic != null && updated.getType() != null) {
            topic = topic.replace(" - " + updated.getType(), "");
        }
        req.setTopic(topic);
        req.setType(updated.getType());
        req.setDifficulty(difficulty);
        req.setChapterId(updated.getChapterId());

        ResourceGenerateMessage message = new ResourceGenerateMessage(id, updated.getUserId(), req);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_RESOURCE,
                RabbitConfig.ROUTING_KEY_GENERATE, message);

        return updated;
    }

    // ========== 同步生成 ==========

    public String generateResource(ResourceGenerateReq req) {
        LearningProfileVO profile = null;
        try {
            profile = profileClient.getProfile(req.getUserId());
        } catch (Exception e) {
            // 画像服务不可用，继续执行
        }

        AiResourceGenReq aiReq = new AiResourceGenReq();
        aiReq.setStudentId(String.valueOf(req.getUserId()));
        aiReq.setChapter(req.getChapter());
        aiReq.setTopic(req.getTopic());
        aiReq.setResourceType(req.getType());
        aiReq.setLevel(req.getDifficulty());
        aiReq.setPrompt(buildPrompt(req, profile));

        if (profile != null) {
            Map<String, Object> profileMap = new HashMap<>();
            profileMap.put("course", profile.getCourse());
            profileMap.put("topic", profile.getTopic());
            profileMap.put("knowledgeBase", profile.getKnowledgeBase());
            profileMap.put("weaknesses", profile.getWeaknesses());
            profileMap.put("pace", profile.getPace());
            profileMap.put("resourcePreference", profile.getResourcePreference());
            profileMap.put("lastScore", profile.getLastScore());
            aiReq.setProfile(profileMap);
        } else {
            aiReq.setProfile(null);
        }

        AiResourceGenResp resp = aiClient.generate(aiReq);
        return resp.getContent();
    }

    private String buildPrompt(ResourceGenerateReq req, LearningProfileVO profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("请生成关于 ").append(req.getTopic()).append(" 的 ").append(req.getType()).append(" 类型资源。");
        sb.append(" 章节：").append(req.getChapter());
        sb.append(" 难度级别：").append(req.getDifficulty());
        if (profile != null) {
            sb.append(" 学生当前课程：").append(profile.getCourse());
            sb.append(" 薄弱点：").append(profile.getWeaknesses());
            sb.append(" 学习节奏：").append(profile.getPace());
        }
        return sb.toString();
    }
}
