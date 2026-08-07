package com.eduagent.teacher.service;

import com.eduagent.teacher.dto.CreateQuestionRequest;
import com.eduagent.teacher.dto.QuestionGenerateRequest;
import com.eduagent.teacher.vo.QuestionVO;

import java.util.List;

public interface QuestionService {

    QuestionVO create(CreateQuestionRequest request);

    List<QuestionVO> list(String chapter, String topic, String type, String difficulty);

    QuestionVO get(Long id);

    QuestionVO update(Long id, CreateQuestionRequest request);

    void delete(Long id);

    /** AI 出题草稿（不落库），教师确认后调 create 落库 */
    List<QuestionVO> generate(QuestionGenerateRequest request);
}
