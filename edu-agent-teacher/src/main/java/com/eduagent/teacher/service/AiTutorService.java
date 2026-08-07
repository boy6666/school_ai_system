package com.eduagent.teacher.service;

import com.eduagent.teacher.dto.AiAskRequest;

import java.util.Map;

public interface AiTutorService {

    /** AI 助教答疑 */
    Map<String, Object> ask(AiAskRequest request);

    /** 学生成绩解读（拉学情+成绩 → ai mode=evaluation） */
    Map<String, Object> explainGrade(Long studentId, Long assignmentId);
}
