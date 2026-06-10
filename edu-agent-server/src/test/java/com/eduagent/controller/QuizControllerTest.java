package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.entity.QuizAnswer;
import com.eduagent.mapper.QuizAnswerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock private QuizAnswerMapper quizAnswerMapper;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private QuizController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getAnswered_ShouldReturnAnswers() {
        QuizAnswer qa = new QuizAnswer();
        qa.setStudentId(1L);
        qa.setResourceId(100L);
        qa.setQuestion("什么是Java?");
        qa.setUserAnswer("A");
        qa.setCorrectAnswer("B");
        qa.setIsCorrect(0);
        qa.setExplanation("Java是一种编程语言");

        when(quizAnswerMapper.selectList(any())).thenReturn(List.of(qa));

        Result<List<Map<String, Object>>> result = controller.getAnswered(100L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("什么是Java?", result.getData().get(0).get("question"));
        assertEquals("B", result.getData().get(0).get("correctAnswer"));
    }

    @Test
    void getAnswered_ShouldReturnEmpty_WhenNoAnswers() {
        when(quizAnswerMapper.selectList(any())).thenReturn(List.of());

        Result<List<Map<String, Object>>> result = controller.getAnswered(999L);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void getWrongQuestions_ShouldReturnWrongAnswers() {
        QuizAnswer qa = new QuizAnswer();
        qa.setId(1L);
        qa.setStudentId(1L);
        qa.setResourceId(100L);
        qa.setQuestion("2+2=?");
        qa.setUserAnswer("3");
        qa.setCorrectAnswer("4");
        qa.setIsCorrect(0);
        qa.setQuestionType("choice");
        qa.setCreateTime(LocalDateTime.now());

        when(quizAnswerMapper.selectList(any())).thenReturn(List.of(qa));

        Result<List<Map<String, Object>>> result = controller.getWrongQuestions();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("2+2=?", result.getData().get(0).get("question"));
        assertEquals("3", result.getData().get(0).get("userAnswer"));
    }

    @Test
    void getWrongQuestions_ShouldReturnEmpty_WhenNoWrongQuestions() {
        when(quizAnswerMapper.selectList(any())).thenReturn(List.of());

        Result<List<Map<String, Object>>> result = controller.getWrongQuestions();

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void getWrongQuestionById_ShouldReturnDetail() {
        QuizAnswer qa = new QuizAnswer();
        qa.setId(1L);
        qa.setStudentId(1L);
        qa.setResourceId(100L);
        qa.setQuestion("排序算法?");
        qa.setUserAnswer("冒泡");
        qa.setCorrectAnswer("快速");
        qa.setIsCorrect(0);
        qa.setExplanation("快速排序更高效");
        qa.setQuestionType("choice");
        qa.setCreateTime(LocalDateTime.now());

        when(quizAnswerMapper.selectOne(any())).thenReturn(qa);

        Result<Map<String, Object>> result = controller.getWrongQuestionById(1L);

        assertEquals(200, result.getCode());
        assertEquals("排序算法?", result.getData().get("question"));
        assertEquals("快速", result.getData().get("correctAnswer"));
    }

    @Test
    void getWrongQuestionById_ShouldReturn404_WhenNotFound() {
        when(quizAnswerMapper.selectOne(any())).thenReturn(null);

        Result<Map<String, Object>> result = controller.getWrongQuestionById(999L);

        assertEquals(404, result.getCode());
    }
}
