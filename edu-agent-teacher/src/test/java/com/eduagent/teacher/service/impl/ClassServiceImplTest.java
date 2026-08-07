package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.CreateClassRequest;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.feign.LearningServiceClient;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.ClassStudentMapper;
import com.eduagent.teacher.vo.ClassVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 班级管理单测（§B.5）：
 * - teacherId 取自 AuthContext，不信任请求体
 * - 加学生幂等（重复 CONFLICT）+ best-effort 回写 learning
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceImplTest {

    @Mock ClassesMapper classesMapper;
    @Mock ClassStudentMapper classStudentMapper;
    @Mock LearningServiceClient learningClient;

    ClassServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassServiceImpl(classesMapper, classStudentMapper, learningClient);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("建班级：teacherId 取 AuthContext")
    void create_setsTeacherIdFromAuth() {
        AuthContext.set("7", "ROLE_TEACHER");
        CreateClassRequest req = new CreateClassRequest();
        req.setName("计科1901");
        req.setCourse("数据结构");
        req.setSemester("2026-春");

        ClassVO vo = service.create(req);

        assertThat(vo.teacherId()).isEqualTo(7L);
        assertThat(vo.name()).isEqualTo("计科1901");
        verify(classesMapper).insert(any(Classes.class));
    }

    @Test
    @DisplayName("建班级未认证 → UNAUTHORIZED")
    void create_noAuth_throwsUnauthorized() {
        assertThatThrownBy(() -> service.create(new CreateClassRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(401));
    }

    @Test
    @DisplayName("加学生成功：写关联表 + 回写 learning")
    void addStudent_ok_writesRelationAndBinds() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.exists(1L, 2L)).thenReturn(0);

        service.addStudent(1L, 2L);

        verify(classStudentMapper).insert(1L, 2L);
        verify(learningClient).bindClass(anyLong(), any());
    }

    @Test
    @DisplayName("重复加学生 → CONFLICT，不写关联表")
    void addStudent_duplicate_throwsConflict() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.exists(1L, 2L)).thenReturn(1);

        assertThatThrownBy(() -> service.addStudent(1L, 2L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(409));
        verify(classStudentMapper, never()).insert(anyLong(), anyLong());
    }

    @Test
    @DisplayName("learning 未就绪告警，不阻断加学生（best-effort）")
    void addStudent_learningDown_stillSucceeds() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.exists(1L, 2L)).thenReturn(0);
        when(learningClient.bindClass(anyLong(), any())).thenThrow(new RuntimeException("learning down"));

        service.addStudent(1L, 2L); // 不应抛异常

        verify(classStudentMapper).insert(1L, 2L);
    }

    @Test
    @DisplayName("他人班级 → FORBIDDEN")
    void addStudent_notOwner_throwsForbidden() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 99L));

        assertThatThrownBy(() -> service.addStudent(1L, 2L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(403));
    }

    @Test
    @DisplayName("本人班级列表：仅返回我的，带学生数")
    void list_returnsOwnClassesWithCount() {
        AuthContext.set("7", "ROLE_TEACHER");
        Classes mine = c(1L, 7L);
        mine.setName("数据结构");
        when(classesMapper.selectList(any())).thenReturn(List.of(mine));
        when(classStudentMapper.selectStudentIds(1L)).thenReturn(List.of(2L, 3L, 4L));

        List<ClassVO> list = service.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).studentCount()).isEqualTo(3);
    }

    private static Classes c(Long id, Long teacherId) {
        Classes c = new Classes();
        c.setId(id);
        c.setTeacherId(teacherId);
        c.setStatus(1);
        return c;
    }
}
