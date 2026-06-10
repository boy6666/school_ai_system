package com.eduagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.Result;
import com.eduagent.entity.QuizAnswer;
import com.eduagent.mapper.QuizAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizAnswerMapper quizAnswerMapper;

    /** 获取学生已作答的题目列表（按 resourceId） */
    @GetMapping("/answered")
    public Result<List<Map<String, Object>>> getAnswered(@RequestParam Long resourceId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<QuizAnswer> answers = quizAnswerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getStudentId, userId)
                        .eq(QuizAnswer::getResourceId, resourceId)
        );

        List<Map<String, Object>> result = answers.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("question", a.getQuestion());
            m.put("userAnswer", a.getUserAnswer());
            m.put("correctAnswer", a.getCorrectAnswer());
            m.put("isCorrect", a.getIsCorrect());
            m.put("explanation", a.getExplanation());
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /** 获取单道历史错题详情 */
    @GetMapping("/wrong-questions/{id}")
    public Result<Map<String, Object>> getWrongQuestionById(@PathVariable Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        QuizAnswer answer = quizAnswerMapper.selectOne(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getId, id)
                        .eq(QuizAnswer::getStudentId, userId)
                        .eq(QuizAnswer::getIsCorrect, 0)
        );

        if (answer == null) {
            return Result.error(404, "错题不存在");
        }

        Map<String, Object> m = new HashMap<>();
        m.put("id", answer.getId());
        m.put("resourceId", answer.getResourceId());
        m.put("question", answer.getQuestion());
        m.put("questionType", answer.getQuestionType());
        m.put("userAnswer", answer.getUserAnswer());
        m.put("correctAnswer", answer.getCorrectAnswer());
        m.put("explanation", answer.getExplanation());
        m.put("createTime", answer.getCreateTime());
        return Result.success(m);
    }

    /** 获取学生所有历史错题（isCorrect=0），按时间降序 */
    @GetMapping("/wrong-questions")
    public Result<List<Map<String, Object>>> getWrongQuestions() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<QuizAnswer> answers = quizAnswerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getStudentId, userId)
                        .eq(QuizAnswer::getIsCorrect, 0)
                        .orderByDesc(QuizAnswer::getCreateTime)
        );

        List<Map<String, Object>> result = answers.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("resourceId", a.getResourceId());
            m.put("question", a.getQuestion());
            m.put("questionType", a.getQuestionType());
            m.put("userAnswer", a.getUserAnswer());
            m.put("correctAnswer", a.getCorrectAnswer());
            m.put("explanation", a.getExplanation());
            m.put("createTime", a.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
