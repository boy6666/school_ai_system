package com.eduagent.code.controller;

import com.eduagent.code.dto.CodeExerciseRequest;
import com.eduagent.code.dto.CodeSubmitRequest;
import com.eduagent.code.service.CodeExerciseService;
import com.eduagent.code.service.SubmissionService;
import com.eduagent.code.vo.CodeExerciseVO;
import com.eduagent.code.vo.CodeSubmitReceiptVO;
import com.eduagent.code.vo.CodeSubmitResultVO;
import com.eduagent.common.feign.UserClient;
import com.eduagent.common.feign.UserInfo;
import com.eduagent.common.result.PageResult;
import com.eduagent.common.result.Result;
import com.eduagent.common.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 代码练习接口（code 服务）。
 * 路径与网关路由 /api/edu-agent-code/** 对齐；/me 演示网关注入身份的透传（AuthContext）。
 * 判分受理口：POST /submit 202 异步 + GET /result/{id}（C1：异步两段式）。
 */
@RestController
@RequestMapping("/api/edu-agent-code")
@RequiredArgsConstructor
public class CodeController {

    private final CodeExerciseService service;
    private final SubmissionService submissionService;
    private final UserClient userClient;

    @PostMapping("/exercises")
    public Result<CodeExerciseVO> create(@Valid @RequestBody CodeExerciseRequest request) {
        return Result.success(service.create(request));
    }

    @GetMapping("/exercises")
    public Result<PageResult<CodeExerciseVO>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.list(page, size));
    }

    /** 判分受理：202 异步，仅返回受理回执 {@link CodeSubmitReceiptVO}（C1） */
    @PostMapping("/submit")
    public ResponseEntity<Result<CodeSubmitReceiptVO>> submit(@Valid @RequestBody CodeSubmitRequest request) {
        CodeSubmitReceiptVO receipt = submissionService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(receipt));
    }

    /** 判分结果查询（C1：GET /api/code/result/{id}） */
    @GetMapping("/result/{id}")
    public Result<CodeSubmitResultVO> result(@PathVariable Long id) {
        return Result.success(submissionService.getResult(id));
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

    /**
     * 跨服务 Feign 示范：经 {@link UserClient} 调用鉴权服务的 /me。
     * 鉴权服务依据本服务透传的 X-User-Id 返回【当前调用方】身份，印证 AuthFeignInterceptor 的透传链。
     */
    @GetMapping("/demo/whoami")
    public Result<UserInfo> whoami() {
        return Result.success(userClient.me());
    }
}
