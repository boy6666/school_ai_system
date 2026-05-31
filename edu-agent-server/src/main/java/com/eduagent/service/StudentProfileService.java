package com.eduagent.service;

import com.eduagent.entity.StudentProfile;
import com.eduagent.entity.User;
import com.eduagent.repository.StudentProfileRepository;
import com.eduagent.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StudentProfileService {

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public StudentProfileService(StudentProfileRepository profileRepository,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> saveOrUpdate(String username, Map<String, Object> profileData) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;

        StudentProfile profile = profileRepository.findByStudentId(user.getId())
                .orElse(new StudentProfile());

        profile.setStudentId(user.getId());
        profile.setMajor(getString(profileData, "major"));
        profile.setGrade(getString(profileData, "grade"));
        profile.setCourse(getString(profileData, "course"));
        profile.setTopic(getString(profileData, "topic"));
        profile.setLearningGoal(getString(profileData, "learning_goal"));
        profile.setKnowledgeBase(getString(profileData, "knowledge_base"));
        profile.setCurrentMastery(getString(profileData, "current_mastery"));
        profile.setCognitiveStyle(getString(profileData, "cognitive_style"));
        profile.setPace(getString(profileData, "pace"));
        profile.setWeaknesses(toJson(getList(profileData, "weaknesses")));
        profile.setMistakePatterns(toJson(getList(profileData, "mistake_patterns")));
        profile.setLearningBehavior(getString(profileData, "learning_behavior"));
        profile.setResourcePreference(toJson(getList(profileData, "resource_preference")));
        profile.setOverallType(getString(profileData, "overall_type"));
        profile.setProfileSuggestions(toJson(getList(profileData, "profile_suggestions")));
        profile.setLastScore(getInt(profileData, "last_score"));
        profile.setLastSuggestion(getString(profileData, "last_suggestion"));

        // 六维层次 — 支持 AI 引擎嵌套对象格式 {level, score, ...}
        profile.setKnowledgeMasteryLevel(getNestedString(profileData, "knowledge_mastery", "level"));
        profile.setLearningGoalClarityLevel(getNestedString(profileData, "learning_goal_clarity", "level"));
        profile.setCognitiveAdaptationLevel(getNestedString(profileData, "cognitive_adaptation", "level"));
        profile.setMistakeAvoidanceLevel(getNestedString(profileData, "mistake_avoidance", "level"));
        profile.setLearningAutonomyLevel(getNestedString(profileData, "learning_autonomy", "level"));
        profile.setOverallLevel(getNestedString(profileData, "overall_level", "level"));

        // 六维分数汇总
        profile.setDimensionScores(buildDimensionScores(profileData));

        // 对话计数
        Integer convCount = getInt(profileData, "conversation_count");
        if (convCount == null) {
            Object cv = profileData.get("conversation_count");
            if (cv instanceof Number) convCount = ((Number) cv).intValue();
        }
        profile.setConversationCount(convCount);

        profileRepository.save(profile);
        return toMap(profile);
    }

    public Map<String, Object> getByUsername(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;

        StudentProfile profile = profileRepository.findByStudentId(user.getId()).orElse(null);
        if (profile == null) return null;

        return toMap(profile);
    }

    private Map<String, Object> toMap(StudentProfile p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("student_id", p.getStudentId());
        map.put("major", p.getMajor());
        map.put("grade", p.getGrade());
        map.put("course", p.getCourse());
        map.put("topic", p.getTopic());
        map.put("learning_goal", p.getLearningGoal());
        map.put("knowledge_base", p.getKnowledgeBase());
        map.put("current_mastery", p.getCurrentMastery());
        map.put("cognitive_style", p.getCognitiveStyle());
        map.put("pace", p.getPace());
        map.put("weaknesses", fromJson(p.getWeaknesses()));
        map.put("mistake_patterns", fromJson(p.getMistakePatterns()));
        map.put("learning_behavior", p.getLearningBehavior());
        map.put("resource_preference", fromJson(p.getResourcePreference()));
        map.put("overall_type", p.getOverallType());
        map.put("profile_suggestions", fromJson(p.getProfileSuggestions()));
        map.put("last_score", p.getLastScore());
        map.put("last_suggestion", p.getLastSuggestion());

        // 六维层次
        map.put("knowledge_mastery_level", p.getKnowledgeMasteryLevel());
        map.put("learning_goal_clarity_level", p.getLearningGoalClarityLevel());
        map.put("cognitive_adaptation_level", p.getCognitiveAdaptationLevel());
        map.put("mistake_avoidance_level", p.getMistakeAvoidanceLevel());
        map.put("learning_autonomy_level", p.getLearningAutonomyLevel());
        map.put("overall_level", p.getOverallLevel());
        map.put("dimension_scores", p.getDimensionScores());
        map.put("conversation_count", p.getConversationCount());

        map.put("create_time", p.getCreateTime() != null ? p.getCreateTime().toString() : null);
        map.put("update_time", p.getUpdateTime() != null ? p.getUpdateTime().toString() : null);
        return map;
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) return (List<String>) val;
        return List.of();
    }

    /**
     * 从嵌套对象中提取字符串字段。AI引擎返回格式如
     * {"knowledge_mastery": {"level": "level_2", "score": 65, ...}}
     */
    @SuppressWarnings("unchecked")
    private String getNestedString(Map<String, Object> map, String key, String subKey) {
        Object val = map.get(key);
        if (val instanceof Map) {
            Object sub = ((Map<String, Object>) val).get(subKey);
            return sub != null ? sub.toString() : null;
        }
        // 兼容扁平格式: knowledge_mastery_level
        Object flat = map.get(key + "_" + subKey);
        return flat != null ? flat.toString() : null;
    }

    /**
     * 从六维嵌套对象构建 dimension_scores JSON。
     * 输入: {"knowledge_mastery": {"score": 65}, "learning_goal_clarity": {"score": 40}, ...}
     * 输出: {"knowledge_mastery":65,"learning_goal_clarity":40,...}
     */
    @SuppressWarnings("unchecked")
    private String buildDimensionScores(Map<String, Object> profileData) {
        String[] dims = {
            "knowledge_mastery", "learning_goal_clarity", "cognitive_adaptation",
            "mistake_avoidance", "learning_autonomy", "overall_level"
        };
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String dim : dims) {
            Object val = profileData.get(dim);
            if (val instanceof Map) {
                Object score = ((Map<String, Object>) val).get("score");
                if (score instanceof Number) {
                    scores.put(dim, ((Number) score).intValue());
                }
            }
        }
        return scores.isEmpty() ? null : toJsonObj(scores);
    }

    private String toJsonObj(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
