package com.eduagent.learning.service;

import com.eduagent.learning.vo.LearningPathVO;
import com.eduagent.learning.vo.PathHistoryVO;

import java.util.List;

public interface LearningPathService {

    LearningPathVO getCurrentPath(Long studentId);

    LearningPathVO generatePath(Long studentId);

    LearningPathVO updateTaskStatus(Long studentId, String stageName, String taskTitle, boolean completed);

    List<PathHistoryVO> getHistory(Long studentId);
}
