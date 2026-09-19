package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.BindClassRequest;
import com.eduagent.learning.dto.SaveProfileRequest;
import com.eduagent.learning.service.ProfileService;
import com.eduagent.learning.vo.ProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学情画像。网关不 StripPrefix，路径带全前缀 /api/edu-agent-learning。
 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 学生查看自己的画像（S） */
    @GetMapping
    public Result<ProfileVO> myProfile() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(profileService.getProfile(studentId));
    }

    /** 教师查看学生画像（T/A） */
    @GetMapping("/{studentId}")
    public Result<ProfileVO> profileOf(@PathVariable("studentId") Long studentId) {
        RoleGuard.requireTeacherOrAdmin();
        return Result.success(profileService.getProfileForTeacher(studentId));
    }

    /** 教师或管理员回写学生班级逻辑引用（T/A） */
    @PostMapping("/{studentId}/class")
    public Result<Void> bindClass(@PathVariable("studentId") Long studentId,
                                  @Valid @RequestBody BindClassRequest request) {
        RoleGuard.requireTeacherOrAdmin();
        profileService.bindClass(studentId, request.getClassId());
        return Result.success();
    }

    /** 学生保存/更新画像基础字段（S） */
    @PostMapping("/save")
    public Result<Map<String, Object>> save(@Valid @RequestBody SaveProfileRequest request) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(profileService.saveProfile(studentId, request));
    }

    /** AI 生成个性化学习建议（S），失败自动降级为固定建议 */
    @PostMapping("/generate-suggestions")
    public Result<Map<String, Object>> generateSuggestions() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(profileService.generateSuggestions(studentId));
    }
}
