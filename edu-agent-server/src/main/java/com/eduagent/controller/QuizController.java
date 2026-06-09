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
}
