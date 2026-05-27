package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.security.SecurityUtils;
import com.eduagent.service.LearningPathService;
import com.eduagent.vo.LearningPathVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/learning-path")
public class LearningPathController {
    @Autowired
    private LearningPathService learningPathService;

    @GetMapping("/current")
    public Result<LearningPathVO> getCurrentPath() {
        Long studentId = SecurityUtils.getCurrentUserId();
        LearningPathVO path = learningPathService.getCurrentPath(studentId);
        return Result.success(path);
    }
}
