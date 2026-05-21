package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.SystemSettingsRequest;
import com.eduagent.security.AdminOperation;
import com.eduagent.service.SystemSettingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/settings")
public class AdminSettingController {

    private final SystemSettingService settingService;

    public AdminSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public Result<Map<String, String>> get() {
        return Result.ok(settingService.getAllSettings());
    }

    @PutMapping
    @AdminOperation(value = "更新系统设置", targetType = "settings")
    public Result<?> update(@RequestBody SystemSettingsRequest request) {
        settingService.updateSettings(request.getSettings());
        return Result.ok("更新成功");
    }
}
