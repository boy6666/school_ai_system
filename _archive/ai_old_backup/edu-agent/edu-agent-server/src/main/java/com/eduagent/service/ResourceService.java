package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.eduagent.entity.Resource;

import java.util.List;
import java.util.Map;

public interface ResourceService extends IService<Resource> {
    Page<Resource> listResources(int page, int pageSize, String keyword, String type, String status);

    /** 根据章节ID和资源类型获取已生成的资源 */
    Resource getByChapterId(Long chapterId, String type);

    /** 获取某章节所有已生成资源 */
    List<Resource> listByChapterId(Long chapterId);

    /** AI生成资源（先调AI，再存DB，返回结果） */
    Resource generateResource(Long chapterId, String chapterName, String topic, String type, String difficulty, Long studentId);

    /** 重新生成资源（换难度） */
    Resource regenerateResource(Long resourceId, String difficulty, Long studentId);

    /** 保存用户反馈 */
    void saveFeedback(Long resourceId, Boolean liked, String difficultyFeedback);

    /** 根据章节和类型查找 */
    Resource findByChapterAndType(Long chapterId, String type);
}
