package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.service.StudentProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
public class StudentProfileController {

    private final StudentProfileService profileService;

    public StudentProfileController(StudentProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{username}")
    public Result<Map<String, Object>> getProfile(@PathVariable String username) {
        Map<String, Object> profile = profileService.getByUsername(username);
        if (profile == null || profile.isEmpty()) {
            return Result.success(Map.of("exists", false));
        }
        profile.put("exists", true);
        return Result.success(profile);
    }

    @PostMapping("/save")
    public Result<Map<String, Object>> saveProfile(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        if (username.isEmpty()) {
            return Result.success(Map.of("success", false, "message", "用户名为空"));
        }
        // TODO: implement save logic
        return Result.success(Map.of("success", true, "message", "保存成功"));
    }
}
