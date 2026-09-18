package com.eduagent.learning.service;

import com.eduagent.learning.dto.SaveProfileRequest;
import com.eduagent.learning.vo.ProfileVO;

import java.util.List;
import java.util.Map;

public interface ProfileService {

    ProfileVO getProfile(Long studentId);

    ProfileVO getProfileForTeacher(Long targetStudentId);

    Map<String, Object> saveProfile(Long studentId, SaveProfileRequest request);

    Map<String, Object> generateSuggestions(Long studentId);

    /**
     * AI 画像合并：learning 是 ai profile_extractor 的唯一落库方。
     * 六维 score 指数移动平均（旧0.6/新0.4），weaknesses/mistakePatterns 去重限长，
     * 引导完成时置 profile_complete=1。
     *
     * @param aiProfile      AI 返回的 profile map（容忍 camelCase / snake_case）
     * @param onboardingDone 本次对话是否完成引导（置 profile_complete=1）
     */
    void mergeAiProfile(Long studentId, Map<String, Object> aiProfile, boolean onboardingDone);

    /** 从画像 profile_data 提取六维平均分（0-100），无数据返回 null */
    Integer averageDimensionScore(Long studentId);

    /** 读取学生画像的 class_id（事件发布用） */
    Long findClassIdByStudentId(Long studentId);

    /** 解析 JSON 数组列（weaknesses 等）为 List，容忍非法 JSON */
    List<String> parseJsonArray(String json);
}
