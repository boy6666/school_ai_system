package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.mapper.*;
import com.eduagent.service.ResourceService;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock private StudyLogMapper studyLogMapper;
    @Mock private StudentProfileMapper studentProfileMapper;
    @Mock private LearningTaskMapper learningTaskMapper;
    @Mock private LearningPathMapper learningPathMapper;
    @Mock private ReportMapper reportMapper;
    @Mock private AiClient aiClient;
    @Mock private ResourceService resourceService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void tasks_ShouldReturnTaskList_WhenDataExists() {
        var mockTask = mock(com.eduagent.entity.LearningTask.class);
        when(mockTask.getId()).thenReturn(1L);
        when(mockTask.getTitle()).thenReturn("完成搜索算法章节学习");
        when(mockTask.getStatus()).thenReturn("doing");
        when(mockTask.getPriority()).thenReturn("high");
        when(learningTaskMapper.selectPendingByUserId(1L)).thenReturn(List.of(mockTask));

        Result<List<Map<String, Object>>> result = controller.tasks();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("完成搜索算法章节学习", result.getData().get(0).get("title"));
        verify(learningTaskMapper).selectPendingByUserId(1L);
    }

    @Test
    void tasks_ShouldReturnEmptyList_WhenNoTasks() {
        when(learningTaskMapper.selectPendingByUserId(1L)).thenReturn(List.of());

        Result<List<Map<String, Object>>> result = controller.tasks();

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void summary_ShouldReturnStats() {
        when(studyLogMapper.totalDuration(1L)).thenReturn(7200);
        List<Map<String, Object>> todayModules = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>();
        m1.put("module", "mindmap");
        m1.put("total", 600);
        todayModules.add(m1);
        when(studyLogMapper.todaySummary(1L)).thenReturn(todayModules);

        Result<Map<String, Object>> result = controller.summary();

        assertEquals(200, result.getCode());
        assertEquals(7200, result.getData().get("totalSec"));
        assertNotNull(result.getData().get("today"));
    }

    @Test
    void summary_ShouldHandleNullDuration() {
        when(studyLogMapper.totalDuration(1L)).thenReturn(null);
        when(studyLogMapper.todaySummary(1L)).thenReturn(List.of());

        Result<Map<String, Object>> result = controller.summary();

        assertEquals(200, result.getCode());
        assertEquals(0, result.getData().get("totalSec"));
        assertNotNull(result.getData().get("today"));
    }

    @Test
    void report_ShouldReturnCompleteReport() {
        when(studyLogMapper.totalDuration(1L)).thenReturn(36000);
        List<Map<String, Object>> modules = new ArrayList<>();
        Map<String, Object> mm = new HashMap<>();
        mm.put("module", "quiz");
        mm.put("total", 1800);
        modules.add(mm);
        when(studyLogMapper.moduleSummary(1L)).thenReturn(modules);
        when(studyLogMapper.dailyTrend(1L)).thenReturn(List.of());

        var sp = mock(com.eduagent.entity.StudentProfile.class);
        when(sp.getLastScore()).thenReturn(85);
        when(sp.getLearningGoal()).thenReturn("掌握Java基础");
        when(sp.getCognitiveStyle()).thenReturn("视觉型");
        when(sp.getPace()).thenReturn("适中");
        when(sp.getTopic()).thenReturn("Java");
        when(sp.getCourse()).thenReturn("Java程序设计");
        when(sp.getProfileData()).thenReturn(null);
        when(sp.getWeaknesses()).thenReturn(null);
        when(sp.getProfileSuggestions()).thenReturn("建议1\n建议2");
        when(learningPathMapper.findByStudentId(1L)).thenReturn(null);
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(sp);

        Result<Map<String, Object>> result = controller.report();

        assertEquals(200, result.getCode());
        assertEquals(36000, result.getData().get("totalSec"));
        assertEquals(85, result.getData().get("score"));
        assertEquals("掌握Java基础", result.getData().get("learning_goal"));
        assertNotNull(result.getData().get("modules"));
        assertNotNull(result.getData().get("profile_suggestions"));
    }

    @Test
    void report_ShouldHandleNoProfile() {
        when(studyLogMapper.totalDuration(1L)).thenReturn(0);
        when(studyLogMapper.moduleSummary(1L)).thenReturn(List.of());
        when(studyLogMapper.dailyTrend(1L)).thenReturn(List.of());
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(null);
        when(learningPathMapper.findByStudentId(1L)).thenReturn(null);

        Result<Map<String, Object>> result = controller.report();

        assertEquals(200, result.getCode());
        assertNull(result.getData().get("score"));
        assertNull(result.getData().get("profile_suggestions"));
    }

    @Test
    void evaluation_ShouldReturnWeaknesses() {
        var sp = mock(com.eduagent.entity.StudentProfile.class);
        when(sp.getWeaknesses()).thenReturn("[\"循环\",\"递归\"]");
        when(sp.getLastScore()).thenReturn(85);
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(sp);

        Result<Map<String, Object>> result = controller.evaluation();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData().get("weaknesses"));
        assertEquals(85, result.getData().get("score"));
    }

    @Test
    void evaluation_ShouldReturnNull_WhenNoProfile() {
        when(studentProfileMapper.findByStudentId(1L)).thenReturn(null);

        Result<Map<String, Object>> result = controller.evaluation();

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }
}
