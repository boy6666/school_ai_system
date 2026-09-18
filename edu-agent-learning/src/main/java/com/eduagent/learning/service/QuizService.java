package com.eduagent.learning.service;

import com.eduagent.learning.dto.JudgeRequest;

import java.util.List;
import java.util.Map;

public interface QuizService {

    List<Map<String, Object>> answered(Long studentId, Long resourceId);

    List<Map<String, Object>> wrongQuestions(Long studentId);

    Map<String, Object> wrongQuestionDetail(Long studentId, Long id);

    Map<String, Object> judge(Long studentId, JudgeRequest request);
}
