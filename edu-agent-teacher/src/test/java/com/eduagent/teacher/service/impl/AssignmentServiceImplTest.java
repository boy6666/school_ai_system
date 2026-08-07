package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.CreateAssignmentRequest;
import com.eduagent.teacher.entity.Assignment;
import com.eduagent.teacher.entity.AssignmentItem;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.entity.Question;
import com.eduagent.teacher.mapper.AssignmentItemMapper;
import com.eduagent.teacher.mapper.AssignmentMapper;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.GradeMapper;
import com.eduagent.teacher.mapper.QuestionMapper;
import com.eduagent.teacher.mq.AssignmentPublishedEvent;
import com.eduagent.teacher.mq.AssignmentPublishedPublisher;
import com.eduagent.teacher.vo.AssignmentVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 作业单测（§B.5）：
 * - 建作业=草稿（status=0）不发事件
 * - publish 置 status=1 并发布 assignment.published
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock AssignmentMapper assignmentMapper;
    @Mock AssignmentItemMapper itemMapper;
    @Mock QuestionMapper questionMapper;
    @Mock ClassesMapper classesMapper;
    @Mock GradeMapper gradeMapper;
    @Mock AssignmentPublishedPublisher publisher;

    ObjectMapper objectMapper = new ObjectMapper();

    AssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentServiceImpl(assignmentMapper, itemMapper, questionMapper,
                classesMapper, gradeMapper, publisher, objectMapper);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("建作业=草稿（status=0），不发发布事件")
    void create_draft_noPublishEvent() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(questionMapper.selectById(100L)).thenReturn(q(100L, "choice"));
        when(assignmentMapper.insert(any(Assignment.class))).thenAnswer(inv -> {
            inv.getArgument(0, Assignment.class).setId(50L);
            return 1;
        });
        when(itemMapper.selectList(any())).thenReturn(List.of());

        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setClassId(1L);
        req.setTitle("第五章作业");
        req.setType("homework");
        CreateAssignmentRequest.ItemReq item = new CreateAssignmentRequest.ItemReq();
        item.setQuestionId(100L);
        item.setScore(10);
        req.setItems(List.of(item));

        AssignmentVO vo = service.create(req);

        assertThat(vo.id()).isEqualTo(50L);
        assertThat(vo.status()).isZero();          // 草稿
        verify(publisher, never()).publish(any()); // 未发布不发事件
        verify(itemMapper).insert(any(AssignmentItem.class));
    }

    @Test
    @DisplayName("非法题目项 → NOT_FOUND")
    void create_missingQuestion_throwsNotFound() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(questionMapper.selectById(999L)).thenReturn(null);

        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setClassId(1L);
        req.setTitle("t");
        req.setType("homework");
        CreateAssignmentRequest.ItemReq item = new CreateAssignmentRequest.ItemReq();
        item.setQuestionId(999L);
        req.setItems(List.of(item));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(404));
    }

    @Test
    @DisplayName("publish：置 status=1 并发布 assignment.published 事件")
    void publish_setsStatusAndSendsEvent() {
        AuthContext.set("7", "ROLE_TEACHER");
        Assignment a = new Assignment();
        a.setId(50L); a.setClassId(1L); a.setTitle("第五章作业"); a.setType("homework");
        a.setCreatorId(7L); a.setStatus(0);
        when(assignmentMapper.selectById(50L)).thenReturn(a);
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(itemMapper.selectList(any())).thenReturn(List.of());

        AssignmentVO vo = service.publish(50L);

        assertThat(vo.status()).isEqualTo(1);

        ArgumentCaptor<AssignmentPublishedEvent> cap = ArgumentCaptor.forClass(AssignmentPublishedEvent.class);
        verify(publisher).publish(cap.capture());
        AssignmentPublishedEvent e = cap.getValue();
        assertThat(e.getAssignmentId()).isEqualTo(50L);
        assertThat(e.getClassId()).isEqualTo(1L);
        assertThat(e.getTitle()).isEqualTo("第五章作业");
    }

    @Test
    @DisplayName("publish 他人班级作业 → FORBIDDEN")
    void publish_notOwner_throwsForbidden() {
        AuthContext.set("7", "ROLE_TEACHER");
        Assignment a = new Assignment();
        a.setId(50L); a.setClassId(1L); a.setCreatorId(99L); a.setStatus(0);
        when(assignmentMapper.selectById(50L)).thenReturn(a);
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 99L)); // 他人班级

        assertThatThrownBy(() -> service.publish(50L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(403));
    }

    private static Classes c(Long id, Long teacherId) {
        Classes c = new Classes();
        c.setId(id);
        c.setTeacherId(teacherId);
        return c;
    }

    private static Question q(Long id, String type) {
        Question q = new Question();
        q.setId(id);
        q.setType(type);
        return q;
    }
}
