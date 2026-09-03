package com.eduagent.learning.controller;

import com.eduagent.common.Result;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.service.StudentProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning")
public class ProfileController {

    @Autowired
    private StudentProfileService profileService;

    @GetMapping("/profile")
    public Result<StudentProfile> getProfile(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        return Result.success(profileService.getByUserId(userId));
    }
}
