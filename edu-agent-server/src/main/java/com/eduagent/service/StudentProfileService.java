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
}
