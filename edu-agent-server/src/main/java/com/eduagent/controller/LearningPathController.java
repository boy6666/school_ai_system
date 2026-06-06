package com.eduagent.controller;

import com.eduagent.common.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import com.eduagent.service.LearningPathService;
import com.eduagent.vo.LearningPathVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/student/learning-path")
@RequiredArgsConstructor
public class LearningPathController {
    private final LearningPathService learningPathService;

    @GetMapping("/current")
    public Result<LearningPathVO> getCurrentPath() {
        Long studentId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("===== [学习路径] 获取当前路径, studentId={} =====", studentId);
        LearningPathVO path = learningPathService.getCurrentPath(studentId);
        return Result.success(path);
    }

    /**
     * AI 重新生成学习路径 → 存 DB → 读 DB → 返回前端
     */
    @PostMapping("/generate")
    public Result<LearningPathVO> generatePath() {
        Long studentId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("===== [学习路径] AI 生成路径, studentId={} =====", studentId);
        LearningPathVO path = learningPathService.generatePath(studentId);
        return Result.success(path);
    }
}
