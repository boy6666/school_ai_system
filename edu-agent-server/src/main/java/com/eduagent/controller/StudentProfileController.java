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
        if (profile == null) {
            return Result.ok(Map.of("exists", false));
        }
        profile.put("exists", true);
        return Result.ok(profile);
    }

    @PostMapping("/save")
    public Result<Map<String, Object>> saveProfile(@RequestBody Map<String, Object> body) {
        String username = (String) body.getOrDefault("username", "");
        if (username.isEmpty()) {
            return Result.fail("用户名不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> profileData = (Map<String, Object>) body.getOrDefault("profile",
                body.containsKey("username") ? body : Map.of());

        Map<String, Object> saved = profileService.saveOrUpdate(username, profileData);
        if (saved == null) {
            return Result.fail("用户不存在");
        }
        return Result.ok("画像保存成功", saved);
    }
}
