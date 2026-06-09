package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.ExplainRequest;
import com.eduagent.service.ExplainService;
import com.eduagent.vo.ExplainResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class ExplainController {

    private final ExplainService explainService;

    @PostMapping("/explain")
    public Result<ExplainResultVO> explain(@RequestBody ExplainRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ExplainResultVO result = explainService.explain(userId, request);
        return Result.success(result);
    }
}
