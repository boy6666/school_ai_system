package com.eduagent.teacher.controller;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.dto.CreateQuestionRequest;
import com.eduagent.teacher.dto.QuestionGenerateRequest;
import com.eduagent.teacher.service.QuestionService;
import com.eduagent.teacher.vo.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题库管理（ROLE_TEACHER）。
 */
@RestController
@RequestMapping("/api/edu-agent-teacher/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public Result<QuestionVO> create(@Valid @RequestBody CreateQuestionRequest request) {
        return Result.success(questionService.create(request));
    }

    @GetMapping
    public Result<List<QuestionVO>> list(@RequestParam(required = false) String chapter,
                                         @RequestParam(required = false) String topic,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String difficulty) {
        return Result.success(questionService.list(chapter, topic, type, difficulty));
    }

    @GetMapping("/{id}")
    public Result<QuestionVO> get(@PathVariable Long id) {
        return Result.success(questionService.get(id));
    }

    @PutMapping("/{id}")
    public Result<QuestionVO> update(@PathVariable Long id, @Valid @RequestBody CreateQuestionRequest request) {
        return Result.success(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    /** AI 出题草稿（不落库） */
    @PostMapping("/generate")
    public Result<List<QuestionVO>> generate(@jakarta.validation.Valid @RequestBody QuestionGenerateRequest request) {
        return Result.success(questionService.generate(request));
    }
}
