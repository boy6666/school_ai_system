package com.eduagent.learning.service.impl;

import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.QuizAnswerMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 画像合并核心逻辑：六维 EMA（旧0.6/新0.4）、去重限长、profile_complete 置位。 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplMergeTest {

    @Mock
    private StudentProfileMapper profileMapper;
    @Mock
    private QuizAnswerMapper quizAnswerMapper;
    @Mock
    private AiServiceClient aiServiceClient;

    private ProfileServiceImpl service;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(profileMapper, quizAnswerMapper,
                aiServiceClient, new AiResultParser(om), om);
    }

    private StudentProfile existing() {
        StudentProfile sp = new StudentProfile();
        sp.setId(1L);
        sp.setStudentId(1001L);
        sp.setTopic("面向对象");
        sp.setWeaknesses("[\"多态\",\"异常处理\"]");
        sp.setProfileData("{\"knowledge_mastery\":{\"score\":70,\"level\":\"level_2\",\"evidence\":[\"旧证据\"]},"
                + "\"overall_level\":{\"score\":64,\"level\":\"level_2\"}}");
        return sp;
    }

    @Test
    void mergeAiProfile_emaAndDedup() throws Exception {
        when(profileMapper.findByStudentId(1001L)).thenReturn(existing());

        Map<String, Object> aiProfile = Map.of(
                "topic", "多线程",
                "weaknesses", List.of("多态", "集合框架"),
                "dimensions", Map.of(
                        "knowledge_mastery", Map.of("score", 90, "level", "level_3"),
                        "overall_level", Map.of("score", 80)));

        service.mergeAiProfile(1001L, aiProfile, false);

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileMapper).updateById(captor.capture());
        StudentProfile saved = captor.getValue();

        assertEquals("多线程", saved.getTopic());
        // 去重合并：旧[多态,异常处理] + 新[多态,集合框架] → 3 项且无重复
        assertEquals(List.of("多态", "异常处理", "集合框架"), om.readValue(saved.getWeaknesses(), List.class));

        Map<String, Object> dims = om.readValue(saved.getProfileData(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> km = (Map<String, Object>) dims.get("knowledge_mastery");
        // EMA：70*0.6 + 90*0.4 = 78
        assertEquals(78, km.get("score"));
        assertEquals("level_3", km.get("level"));
        assertEquals(List.of("旧证据"), km.get("evidence"));

        @SuppressWarnings("unchecked")
        Map<String, Object> overall = (Map<String, Object>) dims.get("overall_level");
        // 64*0.6 + 80*0.4 = 70.4 → 70；lastScore 同步取 overall 分
        assertEquals(70, overall.get("score"));
        assertEquals(70, saved.getLastScore());
    }

    @Test
    void mergeAiProfile_onboardingDoneSetsComplete() {
        when(profileMapper.findByStudentId(anyLong())).thenReturn(null);

        service.mergeAiProfile(1001L, Map.of("course", "JavaSE"), true);

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getProfileComplete());
        assertEquals("JavaSE", captor.getValue().getCourse());
    }

    @Test
    void mergeAiProfile_emptyMapIsNoop() {
        service.mergeAiProfile(1001L, Map.of(), true);
        verify(profileMapper, org.mockito.Mockito.never()).updateById(any(StudentProfile.class));
    }

    @Test
    void parseJsonArray_toleratesLegacyCommaFormat() {
        // 单体遗留的 '[..]' 清洗格式
        assertEquals(List.of("A", "B"), service.parseJsonArray("[\"A\", \"B\"]"));
        assertEquals(List.of("多态"), service.parseJsonArray("多态"));
        assertTrue(service.parseJsonArray(null).isEmpty());
    }
}
