package com.eduagent.service;

import com.eduagent.vo.LearningPathVO;

public interface LearningPathService {
    LearningPathVO getCurrentPath(Long studentId);
    LearningPathVO generatePath(Long studentId);
    LearningPathVO updateTaskStatus(Long studentId, String stageName, String taskTitle, boolean completed);
}
