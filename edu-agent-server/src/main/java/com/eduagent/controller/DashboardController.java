package com.eduagent.controller;
import com.eduagent.security.SecurityUtils;

import com.eduagent.common.Result;
import com.eduagent.service.DashboardService;
import com.eduagent.vo.DashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('student')")
    public Result<DashboardVO> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        DashboardVO vo = dashboardService.getDashboard(userId);
        return Result.success(vo);
    }
}
