package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.eduagent.entity.Resource;

import java.util.List;

public interface ResourceService extends IService<Resource> {
    Page<Resource> listResources(int page, int pageSize, String keyword, String type, String status);
    Resource getByChapterId(Long chapterId, String type);
    List<Resource> listByChapterId(Long chapterId);
    Resource generateResource(Long chapterId, String chapterName, String topic, String type, String difficulty, Long studentId);

    Resource generateResource(Long chapterId, String chapterName, String topic, String type, String difficulty, Long studentId, boolean force);
    Resource regenerateResource(Long resourceId, String difficulty, Long studentId);
    void saveFeedback(Long resourceId, Boolean liked, String difficultyFeedback);
    Resource findByChapterAndType(Long chapterId, String type);
}
