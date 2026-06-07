package com.eduagent.service;

import com.eduagent.vo.LearningPathVO;

public interface LearningPathService {
    LearningPathVO getCurrentPath(Long studentId);
}
