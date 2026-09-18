package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.learning.dto.SaveProfileRequest;
import com.eduagent.learning.dto.ai.AiResourceRequest;
import com.eduagent.learning.entity.QuizAnswer;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.QuizAnswerMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.service.ProfileService;
import com.eduagent.learning.vo.ProfileVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int MAX_LIST_ITEMS = 20;
    private static final List<String> DIMENSION_KEYS = List.of(
            "knowledge_mastery", "learning_goal_clarity", "cognitive_adaptation",
            "mistake_avoidance", "learning_autonomy", "overall_level");

    private final StudentProfileMapper profileMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final AiServiceClient aiServiceClient;
    private final AiResultParser aiResultParser;
    private final ObjectMapper objectMapper;

    @Override
    public ProfileVO getProfile(Long studentId) {
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        return toVO(sp, studentId);
    }

    @Override
    public ProfileVO getProfileForTeacher(Long targetStudentId) {
        return getProfile(targetStudentId);
    }

    @Override
    public Map<String, Object> saveProfile(Long studentId, SaveProfileRequest request) {
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        boolean created = false;
        if (sp == null) {
            sp = new StudentProfile();
            sp.setStudentId(studentId);
            created = true;
        }
        if (request.getPace() != null) sp.setPace(request.getPace());
        if (request.getLearningGoal() != null) sp.setLearningGoal(request.getLearningGoal());
        if (request.getTopic() != null) sp.setTopic(request.getTopic());
        if (request.getCourse() != null) sp.setCourse(request.getCourse());
        if (request.getKnowledgeBase() != null) sp.setKnowledgeBase(request.getKnowledgeBase());
        if (request.getCognitiveStyle() != null) sp.setCognitiveStyle(request.getCognitiveStyle());
        if (request.getOverallType() != null) sp.setOverallType(request.getOverallType());
        if (request.getWeaknesses() != null) sp.setWeaknesses(toJson(request.getWeaknesses()));
        if (request.getResourcePreference() != null) sp.setResourcePreference(toJson(request.getResourcePreference()));
        if (request.getMistakePatterns() != null) sp.setMistakePatterns(toJson(request.getMistakePatterns()));
        sp.setUpdateTime(LocalDateTime.now());

        if (created) {
            profileMapper.insert(sp);
        } else {
            profileMapper.updateById(sp);
        }
        log.info("[Profile] 画像保存 studentId={}, id={}, 新建={}", studentId, sp.getId(), created);
        return Map.of("id", sp.getId(), "status", "saved");
    }

    @Override
    public Map<String, Object> generateSuggestions(Long studentId) {
        StudentProfile sp = profileMapper.findByStudentId(studentId);

        StringBuilder profileInfo = new StringBuilder();
        if (sp != null) {
            profileInfo.append("学生画像信息：\n");
            appendIfNotNull(profileInfo, "课程", sp.getCourse());
            appendIfNotNull(profileInfo, "当前主题", sp.getTopic());
            appendIfNotNull(profileInfo, "学习节奏", sp.getPace());
            appendIfNotNull(profileInfo, "学习目标", sp.getLearningGoal());
            appendIfNotNull(profileInfo, "知识基础", sp.getKnowledgeBase());
            appendIfNotNull(profileInfo, "薄弱点", sp.getWeaknesses());
            appendIfNotNull(profileInfo, "认知风格", sp.getCognitiveStyle());
            appendIfNotNull(profileInfo, "综合类型", sp.getOverallType());
        } else {
            profileInfo.append("新用户，暂无画像数据。\n");
        }
        String prompt = profileInfo
                + "\n请根据以上学生画像，生成4条个性化的学习建议。\n"
                + "要求返回 JSON：{\"suggestions\": [\"建议1\", \"建议2\", \"建议3\", \"建议4\"]}\n"
                + "建议要具体、可执行，针对学生的薄弱点和学习目标。纯 JSON 不要 markdown。";

        List<String> suggestions = new ArrayList<>();
        try {
            AiResourceRequest req = AiResourceRequest.builder()
                    .studentId(String.valueOf(studentId))
                    .resourceType("suggestion")
                    .mode("suggestion")
                    .prompt(prompt)
                    .build();
            suggestions = aiResultParser.parseSuggestions(aiServiceClient.generateResource(req));
        } catch (Exception e) {
            log.warn("[Profile] AI 建议生成失败，走 fallback: {}", e.getMessage());
        }
        if (suggestions.isEmpty()) {
            suggestions = List.of(
                    "从基础概念开始，系统学习核心知识点",
                    "每天坚持30分钟代码实践",
                    "使用思维导图梳理知识体系",
                    "多做练习，巩固薄弱环节");
        }

        if (sp != null) {
            sp.setProfileSuggestions(String.join("\n", suggestions));
            sp.setUpdateTime(LocalDateTime.now());
            profileMapper.updateById(sp);
        }
        return Map.of("suggestions", suggestions);
    }

    @Override
    public void mergeAiProfile(Long studentId, Map<String, Object> aiProfile, boolean onboardingDone) {
        if (aiProfile == null || aiProfile.isEmpty()) {
            return;
        }
        try {
            StudentProfile sp = profileMapper.findByStudentId(studentId);
            boolean created = false;
            if (sp == null) {
                sp = new StudentProfile();
                sp.setStudentId(studentId);
                created = true;
            }

            String topic = str(aiProfile, "topic");
            if (topic != null) sp.setTopic(topic);
            String course = str(aiProfile, "course");
            if (course != null) sp.setCourse(course);
            String goal = str(aiProfile, "learningGoal", "learning_goal");
            if (goal != null) sp.setLearningGoal(goal);
            String kb = str(aiProfile, "knowledgeBase", "knowledge_base");
            if (kb != null) sp.setKnowledgeBase(kb);
            String style = str(aiProfile, "cognitiveStyle", "cognitive_style");
            if (style != null) sp.setCognitiveStyle(style);
            String pace = str(aiProfile, "pace");
            if (pace != null) sp.setPace(pace);
            String overallType = str(aiProfile, "overallType", "overall_type");
            if (overallType != null) sp.setOverallType(overallType);

            List<String> weaknesses = mergeList(parseJsonArray(sp.getWeaknesses()),
                    rawList(aiProfile.get(firstKey(aiProfile, "weaknesses"))));
            if (!weaknesses.isEmpty()) sp.setWeaknesses(toJson(weaknesses));

            List<String> mistakes = mergeList(parseJsonArray(sp.getMistakePatterns()),
                    rawList(aiProfile.get(firstKey(aiProfile, "mistakePatterns", "mistake_patterns"))));
            if (!mistakes.isEmpty()) sp.setMistakePatterns(toJson(mistakes));

            Map<String, Object> dimensions = firstMap(aiProfile, "dimensions");
            if (dimensions != null && !dimensions.isEmpty()) {
                String merged = mergeDimensions(sp.getProfileData(), dimensions);
                if (merged != null) {
                    sp.setProfileData(merged);
                    // lastScore 跟随合并后的 overall_level（EMA 结果），而非 AI 原始分
                    Integer overall = extractOverallScore(objectMapper.readValue(merged,
                            new TypeReference<Map<String, Object>>() { }));
                    if (overall != null) sp.setLastScore(overall);
                }
            }
            Integer lastScore = intVal(aiProfile.get(firstKey(aiProfile, "lastScore", "last_score")));
            if (lastScore != null && sp.getLastScore() == null) sp.setLastScore(lastScore);

            if (onboardingDone) {
                sp.setProfileComplete(1);
            }
            sp.setUpdateTime(LocalDateTime.now());
            if (created) {
                profileMapper.insert(sp);
            } else {
                profileMapper.updateById(sp);
            }
            log.info("[Profile] AI 画像合并完成 studentId={}, 引导完成={}", studentId, onboardingDone);
        } catch (Exception e) {
            log.error("[Profile] AI 画像合并失败 studentId={}: {}", studentId, e.getMessage(), e);
        }
    }

    @Override
    public Integer averageDimensionScore(Long studentId) {
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp == null || sp.getProfileData() == null) {
            return null;
        }
        try {
            Map<String, Object> dims = objectMapper.readValue(sp.getProfileData(),
                    new TypeReference<Map<String, Object>>() { });
            return averageOf(dims);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long findClassIdByStudentId(Long studentId) {
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        return sp != null ? sp.getClassId() : null;
    }

    @Override
    public List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            // 兼容单体遗留的非 JSON 逗号分隔格式
            List<String> out = new ArrayList<>();
            for (String s : json.replaceAll("[\\[\\]\"]", "").split("\\s*,\\s*")) {
                if (!s.isBlank()) out.add(s.trim());
            }
            return out;
        }
    }

    // ---------- 私有方法 ----------

    private ProfileVO toVO(StudentProfile sp, Long studentId) {
        ProfileVO vo = new ProfileVO();
        if (sp == null) {
            vo.setStudentId(studentId);
            vo.setExists(false);
            return vo;
        }
        vo.setStudentId(sp.getStudentId());
        vo.setClassId(sp.getClassId());
        vo.setMajor(sp.getMajor());
        vo.setGrade(sp.getGrade());
        vo.setCourse(sp.getCourse());
        vo.setTopic(sp.getTopic());
        vo.setLearningGoal(sp.getLearningGoal());
        vo.setKnowledgeBase(sp.getKnowledgeBase());
        vo.setCognitiveStyle(sp.getCognitiveStyle());
        vo.setPace(sp.getPace());
        vo.setWeaknesses(parseJsonArray(sp.getWeaknesses()));
        vo.setMistakePatterns(parseJsonArray(sp.getMistakePatterns()));
        vo.setResourcePreference(parseJsonArray(sp.getResourcePreference()));
        vo.setOverallType(sp.getOverallType());
        vo.setLastScore(sp.getLastScore());
        vo.setProfileComplete(sp.getProfileComplete() != null && sp.getProfileComplete() == 1);
        if (sp.getProfileData() != null) {
            try {
                vo.setDimensions(objectMapper.readValue(sp.getProfileData(),
                        new TypeReference<Map<String, Object>>() { }));
            } catch (Exception ignored) { }
        }
        if (sp.getProfileSuggestions() != null) {
            vo.setProfileSuggestions(List.of(sp.getProfileSuggestions().split("\\n")));
        }
        vo.setLastSuggestion(sp.getLastSuggestion());
        vo.setUpdateTime(sp.getUpdateTime());
        vo.setExists(true);
        try {
            vo.setQuizCount(quizAnswerMapper.selectCount(
                    new LambdaQueryWrapper<QuizAnswer>().eq(QuizAnswer::getStudentId, studentId)));
        } catch (Exception ignored) { }
        return vo;
    }

    /** 六维合并：旧分 0.6 + 新分 0.4 的指数移动平均，保留 evidence */
    private String mergeDimensions(String oldJson, Map<String, Object> incoming) {
        try {
            Map<String, Object> merged = oldJson != null
                    ? objectMapper.readValue(oldJson, new TypeReference<Map<String, Object>>() { })
                    : new LinkedHashMap<>();
            for (String key : DIMENSION_KEYS) {
                Object val = incoming.get(key);
                if (!(val instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> newDim = (Map<String, Object>) val;
                Integer newScore = intVal(newDim.get("score"));
                if (newScore == null) {
                    continue;
                }
                Map<String, Object> out = new LinkedHashMap<>();
                Object old = merged.get(key);
                Integer oldScore = null;
                if (old instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> oldDim = (Map<String, Object>) old;
                    oldScore = intVal(oldDim.get("score"));
                    Object evidence = oldDim.get("evidence");
                    if (evidence != null) out.put("evidence", evidence);
                }
                int finalScore = oldScore == null ? newScore
                        : (int) Math.round(oldScore * 0.6 + newScore * 0.4);
                out.put("score", finalScore);
                out.put("level", str(newDim, "level") != null ? str(newDim, "level")
                        : levelOf(finalScore));
                if (!out.containsKey("evidence")) {
                    Object ev = newDim.get("evidence");
                    if (ev != null) out.put("evidence", ev);
                }
                merged.put(key, out);
            }
            return objectMapper.writeValueAsString(merged);
        } catch (Exception e) {
            log.warn("[Profile] 六维合并失败: {}", e.getMessage());
            return null;
        }
    }

    private Integer extractOverallScore(Map<String, Object> dimensions) {
        Object overall = dimensions.get("overall_level");
        if (overall instanceof Map) {
            return intVal(((Map<?, ?>) overall).get("score"));
        }
        return null;
    }

    private Integer averageOf(Map<String, Object> dims) {
        int sum = 0, n = 0;
        for (String key : DIMENSION_KEYS) {
            if (dims.get(key) instanceof Map) {
                Integer s = intVal(((Map<?, ?>) dims.get(key)).get("score"));
                if (s != null) { sum += s; n++; }
            }
        }
        return n == 0 ? null : sum / n;
    }

    private String levelOf(int score) {
        if (score >= 80) return "level_3";
        if (score >= 60) return "level_2";
        return "level_1";
    }

    private List<String> mergeList(List<String> oldList, List<String> newList) {
        List<String> merged = new ArrayList<>(oldList);
        for (String item : newList) {
            if (item != null && !item.isBlank() && !merged.contains(item)) {
                merged.add(item.trim());
            }
        }
        while (merged.size() > MAX_LIST_ITEMS) {
            merged.remove(merged.size() - 1);
        }
        return merged;
    }

    private List<String> rawList(Object val) {
        List<String> out = new ArrayList<>();
        if (val instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) out.add(String.valueOf(o));
            }
        } else if (val instanceof String s && !s.isBlank()) {
            out.add(s);
        }
        return out;
    }

    private String str(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && map.get(k) != null) {
                String v = String.valueOf(map.get(k)).trim();
                if (!v.isEmpty() && !"null".equals(v)) return v;
            }
        }
        return null;
    }

    /** 返回 map 中第一个存在的 key（camelCase/snake_case 兼容） */
    private String firstKey(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) return k;
        }
        return keys[0];
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstMap(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private Integer intVal(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return (int) Double.parseDouble(s); } catch (Exception ignored) { }
        }
        return null;
    }

    private String toJson(Object val) {
        try {
            return objectMapper.writeValueAsString(val);
        } catch (Exception e) {
            return null;
        }
    }

    private void appendIfNotNull(StringBuilder sb, String label, String val) {
        if (val != null && !val.isBlank()) {
            sb.append(label).append("：").append(val).append("\n");
        }
    }
}
