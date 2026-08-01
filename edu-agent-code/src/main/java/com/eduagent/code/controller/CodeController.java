package com.eduagent.code.controller;

import com.eduagent.code.dto.CodeExerciseRequest;
import com.eduagent.code.service.CodeExerciseService;
import com.eduagent.code.vo.CodeExerciseVO;
import com.eduagent.common.result.PageResult;
import com.eduagent.common.result.Result;
import com.eduagent.common.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 代码练习接口（code 服务基础骨架）。
 * 路径与网关路由 /api/edu-agent-code/** 对齐；/me 演示网关注入身份的透传（AuthContext）。
 */
@RestController
@RequestMapping("/api/edu-agent-code")
@RequiredArgsConstructor
public class CodeController {

    private final CodeExerciseService service;

    @PostMapping("/exercises")
    public Result<CodeExerciseVO> create(@Valid @RequestBody CodeExerciseRequest request) {
        return Result.success(service.create(request));
    }

    @GetMapping("/exercises")
    public Result<PageResult<CodeExerciseVO>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.list(page, size));
    }

    @GetMapping("/exercises/{id}")
    public Result<CodeExerciseVO> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(Map.of(
                "userId", AuthContext.getUserId(),
                "roles", AuthContext.getRoles()));
    }
}
