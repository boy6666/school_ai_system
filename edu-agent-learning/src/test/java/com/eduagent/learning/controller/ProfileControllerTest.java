package com.eduagent.learning.controller;

import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.learning.exception.GlobalExceptionHandler;
import com.eduagent.learning.security.AuthHeaderFilter;
import com.eduagent.learning.service.ProfileService;
import com.eduagent.learning.vo.ProfileVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class,
        properties = "spring.cloud.nacos.config.import-check.enabled=false")
@ContextConfiguration(classes = ProfileController.class)
@Import({AuthHeaderFilter.class, GlobalExceptionHandler.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @Test
    void studentCanReadOwnProfile() throws Exception {
        ProfileVO profile = new ProfileVO();
        profile.setStudentId(1001L);
        when(profileService.getProfile(1001L)).thenReturn(profile);

        mockMvc.perform(get("/api/edu-agent-learning/profile")
                        .header(ServiceConstants.HEADER_USER_ID, "1001")
                        .header(ServiceConstants.HEADER_USER_ROLES, ServiceConstants.ROLE_STUDENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.studentId").value(1001));
    }

    @Test
    void teacherCannotUseStudentSelfServiceEndpoint() throws Exception {
        mockMvc.perform(get("/api/edu-agent-learning/profile")
                        .header(ServiceConstants.HEADER_USER_ID, "9")
                        .header(ServiceConstants.HEADER_USER_ROLES, ServiceConstants.ROLE_TEACHER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void roleWithoutUserIdIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/edu-agent-learning/profile/1001")
                        .header(ServiceConstants.HEADER_USER_ROLES, ServiceConstants.ROLE_TEACHER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void teacherCanBindStudentClass() throws Exception {
        mockMvc.perform(post("/api/edu-agent-learning/profile/1001/class")
                        .header(ServiceConstants.HEADER_USER_ID, "9")
                        .header(ServiceConstants.HEADER_USER_ROLES, ServiceConstants.ROLE_TEACHER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(profileService).bindClass(1001L, 8L);
    }

    @Test
    void bindClassRejectsInvalidClassId() throws Exception {
        mockMvc.perform(post("/api/edu-agent-learning/profile/1001/class")
                        .header(ServiceConstants.HEADER_USER_ID, "9")
                        .header(ServiceConstants.HEADER_USER_ROLES, ServiceConstants.ROLE_TEACHER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
