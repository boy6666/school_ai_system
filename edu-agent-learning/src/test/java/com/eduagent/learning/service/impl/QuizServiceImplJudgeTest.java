package com.eduagent.learning.service.impl;

import com.eduagent.learning.dto.JudgeRequest;
import com.eduagent.learning.entity.QuizAnswer;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.QuizAnswerMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 判分矩阵：选择题本地比对（含多选乱序）、主观题 AI judge、AI 不可用降级。 */
@ExtendWith(MockitoExtension.class)
class QuizServiceImplJudgeTest {

    @Mock
    private QuizAnswerMapper quizAnswerMapper;
    @Mock
    private StudentProfileMapper profileMapper;
    @Mock
    private AiServiceClient aiServiceClient;

    private QuizServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QuizServiceImpl(quizAnswerMapper, profileMapper,
                aiServiceClient, new AiResultParser(new ObjectMapper()));
    }

    private JudgeRequest choice(String user, String correct) {
        JudgeRequest r = new JudgeRequest();
        r.setQuestion("接口中能否定义成员变量？");
        r.setQuestionType("choice");
        r.setUserAnswer(user);
        r.setCorrectAnswer(correct);
        return r;
    }

    @Test
    void judge_choiceLocalCompare_correct() {
        Map<String, Object> out = service.judge(1001L, choice("A", "A"));
        assertEquals(1, out.get("isCorrect"));
        assertEquals(1, out.get("score"));
        // 选择题不调 AI
        verify(aiServiceClient, never()).generateResource(any());
    }

    @Test
    void judge_choiceLocalCompare_wrong() {
        Map<String, Object> out = service.judge(1001L, choice("B", "A"));
        assertEquals(0, out.get("isCorrect"));
        assertTrue(out.get("comment").toString().contains("A"));
    }

    @Test
    void judge_multiChoiceOrderInsensitive() {
        // 多选 "C,A" 与 "AC" 视为一致（去标点+排序）
        Map<String, Object> out = service.judge(1001L, choice("C,A", "AC"));
        assertEquals(1, out.get("isCorrect"));
    }

    @Test
    void judge_subjectiveViaAi() {
        when(profileMapper.findByStudentId(1001L)).thenReturn(new StudentProfile());
        when(aiServiceClient.generateResource(any())).thenReturn(
                "{\"code\":0,\"data\":{\"score\":1,\"correct\":true,\"comment\":\"要点覆盖\",\"explanation\":\"多态=运行时绑定\"}}");

        JudgeRequest r = new JudgeRequest();
        r.setQuestion("简述多态的概念。");
        r.setQuestionType("short");
        r.setUserAnswer("同一方法不同实现");
        r.setCorrectAnswer(null);

        Map<String, Object> out = service.judge(1001L, r);
        assertEquals(1, out.get("isCorrect"));
        assertEquals("要点覆盖", out.get("comment"));

        ArgumentCaptor<QuizAnswer> captor = ArgumentCaptor.forClass(QuizAnswer.class);
        verify(quizAnswerMapper).insert(captor.capture());
        assertEquals("多态=运行时绑定", captor.getValue().getExplanation());
    }

    @Test
    void judge_aiDown_degradesToNullVerdict() {
        when(profileMapper.findByStudentId(1001L)).thenReturn(null);
        when(aiServiceClient.generateResource(any())).thenThrow(new RuntimeException("connect refused"));

        JudgeRequest r = new JudgeRequest();
        r.setQuestion("写一个单例模式。");
        r.setQuestionType("code");
        r.setUserAnswer("class S{}");

        Map<String, Object> out = service.judge(1001L, r);
        assertNull(out.get("isCorrect"));
        assertNull(out.get("score"));
        assertTrue(out.get("comment").toString().contains("AI"));

        ArgumentCaptor<QuizAnswer> captor = ArgumentCaptor.forClass(QuizAnswer.class);
        verify(quizAnswerMapper).insert(captor.capture());
        assertNull(captor.getValue().getIsCorrect());
    }
}
