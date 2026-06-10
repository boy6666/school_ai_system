package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.entity.StudentProfile;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.mapper.QuizAnswerMapper;
import com.eduagent.agent.AiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock private StudentProfileMapper studentProfileMapper;
    @Mock private QuizAnswerMapper quizAnswerMapper;
    @Mock private AiClient aiClient;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProfileController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    // ==================== GET /profile/{id} ====================
    @Test
    void getById_ShouldReturnProfile_WhenExists() {
        StudentProfile sp = new StudentProfile();
        sp.setId(10L);
        sp.setStudentId(1L);
        sp.setCourse("Java程序设计");
        sp.setTopic("Java基础");
        sp.setLearningGoal("掌握Java核心");
        sp.setCognitiveStyle("视觉型");
        sp.setPace("适中");
        sp.setLastScore(85);
        sp.setProfileData("{\"knowledge_mastery\":{\"score\":80}}");
        sp.setWeaknesses("[\"循环\",\"递归\"]");
        sp.setUpdateTime(LocalDateTime.now());
        sp.setLastSuggestion("坚持每天练习");

        when(studentProfileMapper.findByStudentId(1L)).thenReturn(sp);
        when(quizAnswerMapper.selectCount(any())).thenReturn(10L);

        Result<Map<String, Object>> result = controller.getById(1L);

        assertEquals(200, result.getCode());
        Map<String, Object> data = result.getData();
        assertEquals(10L, data.get("id"));
        assertEquals("Java程序设计", data.get("course"));
        assertEquals("掌握Java核心", data.get("learning_goal"));
        assertTrue((Boolean) data.get("exists"));
        assertEquals(10L, data.get("quizCount"));
    }

    @Test
    void getById_ShouldReturnExistsFalse_WhenNotFound() {
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(null);

        Result<Map<String, Object>> result = controller.getById(1L);

        assertEquals(200, result.getCode());
        assertFalse((Boolean) result.getData().get("exists"));
    }

    // ==================== POST /profile/save ====================
    @Test
    void save_ShouldCreateNewProfile_WhenNotExists() {
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(null);
        // Mock insert to set the auto-generated ID (MyBatis-Plus behavior)
        doAnswer(invocation -> {
            StudentProfile sp = invocation.getArgument(0);
            sp.setId(100L);
            return 1;
        }).when(studentProfileMapper).insert(any(StudentProfile.class));

        Map<String, Object> body = new HashMap<>();
        body.put("pace", "快速");
        body.put("learning_goal", "掌握Java");
        body.put("topic", "Java基础");
        body.put("course", "Java程序设计");
        body.put("cognitive_style", "视觉型");
        body.put("weaknesses", List.of("循环", "递归"));
        Map<String, Object> dimensions = new HashMap<>();
        Map<String, Object> overall = new HashMap<>();
        overall.put("score", 75);
        dimensions.put("overall_level", overall);
        body.put("dimensions", dimensions);

        Result<Map<String, Object>> result = controller.save(body);

        assertEquals(200, result.getCode());
        assertEquals("saved", result.getData().get("status"));
        assertEquals(100L, result.getData().get("id"));
        verify(studentProfileMapper).insert(any(StudentProfile.class));
    }

    @Test
    void save_ShouldUpdateExistingProfile_WhenExists() {
        StudentProfile existing = new StudentProfile();
        existing.setId(10L);
        existing.setStudentId(1L);
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(existing);

        Map<String, Object> body = new HashMap<>();
        body.put("pace", "慢速");
        body.put("learning_goal", "夯实基础");
        body.put("topic", "数据结构");

        Result<Map<String, Object>> result = controller.save(body);

        assertEquals(200, result.getCode());
        assertEquals("saved", result.getData().get("status"));
        verify(studentProfileMapper).updateById(any(StudentProfile.class));
    }

    // ==================== POST /profile/generate-suggestions ====================
    @Test
    void generateSuggestions_ShouldReturnSuggestions() {
        StudentProfile sp = new StudentProfile();
        sp.setCourse("Java");
        sp.setTopic("面向对象");
        sp.setPace("适中");
        sp.setLearningGoal("掌握OOP");
        sp.setWeaknesses("[\"继承\",\"多态\"]");
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(sp);
        when(aiClient.post(anyString(), any())).thenReturn(
            "{\"content\":\"{\\\"suggestions\\\":[\\\"多练习继承\\\",\\\"理解多态\\\"]}\"}"
        );

        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);

        Result<Map<String, Object>> result = controller.generateSuggestions(body);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData().get("suggestions"));
    }

    @Test
    void generateSuggestions_ShouldReturnFallback_WhenAiFails() {
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(null);
        when(aiClient.post(anyString(), any())).thenReturn("{}");

        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);

        Result<Map<String, Object>> result = controller.generateSuggestions(body);

        assertEquals(200, result.getCode());
        List<String> suggestions = (List<String>) result.getData().get("suggestions");
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.get(0).contains("基础概念"));
    }
}
