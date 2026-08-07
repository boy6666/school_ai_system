package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.SubmitAssignmentRequest;
import com.eduagent.teacher.dto.UpdateGradeRequest;
import com.eduagent.teacher.entity.Assignment;
import com.eduagent.teacher.entity.AssignmentItem;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.entity.Grade;
import com.eduagent.teacher.entity.Question;
import com.eduagent.teacher.feign.CodeServiceClient;
import com.eduagent.teacher.mapper.AssignmentItemMapper;
import com.eduagent.teacher.mapper.AssignmentMapper;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.ClassStudentMapper;
import com.eduagent.teacher.mapper.GradeMapper;
import com.eduagent.teacher.mapper.QuestionMapper;
import com.eduagent.teacher.vo.GradeVO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提交批改核心单测（§B.5）：
 * - choice/blank 本地判分，code 走异步两段式（受理、不判分）
 * - 属主/权限守卫、幂等 upsert
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    @Mock AssignmentMapper assignmentMapper;
    @Mock AssignmentItemMapper itemMapper;
    @Mock QuestionMapper questionMapper;
    @Mock GradeMapper gradeMapper;
    @Mock ClassesMapper classesMapper;
    @Mock ClassStudentMapper classStudentMapper;
    @Mock CodeServiceClient codeClient;

    GradeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GradeServiceImpl(assignmentMapper, itemMapper, questionMapper,
                gradeMapper, classesMapper, classStudentMapper, codeClient);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ── submit ──

    @Test
    @DisplayName("choice 题本地判分：答案正确得满分，status=1，不调 code")
    void submit_choiceCorrect_scoresFull_andNoCodeCall() {
        AuthContext.set("1", "ROLE_STUDENT");
        Assignment a = a(1L);
        when(assignmentMapper.selectById(1L)).thenReturn(a);

        AssignmentItem item = item(10L, 1L, 100L, "choice", 10);
        when(itemMapper.selectById(10L)).thenReturn(item);
        when(questionMapper.selectById(100L)).thenReturn(q("choice", "B"));
        when(gradeMapper.selectByStuItem(1L, 1L, 10L)).thenReturn(null);

        SubmitAssignmentRequest req = new SubmitAssignmentRequest();
        SubmitAssignmentRequest.ItemReq ir = new SubmitAssignmentRequest.ItemReq();
        ir.setItemId(10L);
        ir.setSubmission("B");
        req.setItems(List.of(ir));

        List<GradeVO> result = service.submit(1L, req);

        assertThat(result).hasSize(1);
        GradeVO gv = result.get(0);
        assertThat(gv.score()).isEqualTo(10);
        assertThat(gv.status()).isEqualTo(1);
        verify(codeClient, never()).submit(any());
        verify(gradeMapper).insert(any(Grade.class));
    }

    @Test
    @DisplayName("choice 题答案错误：0 分，status=1")
    void submit_choiceWrong_scoresZero() {
        AuthContext.set("1", "ROLE_STUDENT");
        when(assignmentMapper.selectById(1L)).thenReturn(a(1L));
        when(itemMapper.selectById(10L)).thenReturn(item(10L, 1L, 100L, "choice", 10));
        when(questionMapper.selectById(100L)).thenReturn(q("choice", "B"));
        when(gradeMapper.selectByStuItem(1L, 1L, 10L)).thenReturn(null);

        SubmitAssignmentRequest req = new SubmitAssignmentRequest();
        SubmitAssignmentRequest.ItemReq ir = new SubmitAssignmentRequest.ItemReq();
        ir.setItemId(10L);
        ir.setSubmission("C");
        req.setItems(List.of(ir));

        GradeVO gv = service.submit(1L, req).get(0);
        assertThat(gv.score()).isZero();
        assertThat(gv.status()).isEqualTo(1);
        verify(gradeMapper).insert(any(Grade.class));
    }

    @Test
    @DisplayName("code 题异步两段式：status=0 待批，仅受理不判分")
    void submit_code_writesPending_andAcceptsReceipt() {
        AuthContext.set("1", "ROLE_STUDENT");
        when(assignmentMapper.selectById(1L)).thenReturn(a(1L));
        when(itemMapper.selectById(20L)).thenReturn(item(20L, 1L, 200L, "code", 100));
        when(questionMapper.selectById(200L)).thenReturn(q("code", "expected"));
        when(gradeMapper.selectByStuItem(1L, 1L, 20L)).thenReturn(null);

        SubmitAssignmentRequest req = new SubmitAssignmentRequest();
        SubmitAssignmentRequest.ItemReq ir = new SubmitAssignmentRequest.ItemReq();
        ir.setItemId(20L);
        ir.setSubmission("public class A {}");
        ir.setLanguage("java");
        req.setItems(List.of(ir));

        GradeVO gv = service.submit(1L, req).get(0);

        assertThat(gv.status()).isZero();          // 待批，等 assignment.graded 回填
        assertThat(gv.gradedAt()).isNull();
        verify(codeClient).submit(any());          // 提交受理
        verify(gradeMapper).insert(any(Grade.class));
    }

    @Test
    @DisplayName("code 题受理失败不阻断提交（best-effort）")
    void submit_code_receiptFailure_stillPersists() {
        AuthContext.set("1", "ROLE_STUDENT");
        when(assignmentMapper.selectById(1L)).thenReturn(a(1L));
        when(itemMapper.selectById(20L)).thenReturn(item(20L, 1L, 200L, "code", 100));
        when(questionMapper.selectById(200L)).thenReturn(q("code", "expected"));
        when(gradeMapper.selectByStuItem(1L, 1L, 20L)).thenReturn(null);
        // code 服务未就绪/超时：submit 抛异常，应被捕获，成绩仍落库（待批）
        when(codeClient.submit(any())).thenThrow(new RuntimeException("code down"));

        GradeVO gv = service.submit(1L, reqWith(20L, "code")).get(0);

        assertThat(gv.status()).isZero();
        verify(gradeMapper).insert(any(Grade.class));
    }

    @Test
    @DisplayName("作业不存在 → NOT_FOUND")
    void submit_assignmentMissing_throwsNotFound() {
        AuthContext.set("1", "ROLE_STUDENT");
        when(assignmentMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.submit(1L, reqWith(10L, "choice")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(404));
    }

    @Test
    @DisplayName("未认证提交 → UNAUTHORIZED")
    void submit_noAuth_throwsUnauthorized() {
        // 未设置 AuthContext
        assertThatThrownBy(() -> service.submit(1L, reqWith(10L, "choice")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(401));
    }

    // ── 教师复核 ──

    @Test
    @DisplayName("教师复核成绩：覆盖 score/comment/status=1")
    void updateGrade_teacher_overrides() {
        AuthContext.set("10", "ROLE_TEACHER");
        Grade g = new Grade();
        g.setId(5L); g.setAssignmentId(1L); g.setStudentId(2L); g.setScore(5);
        when(gradeMapper.selectById(5L)).thenReturn(g);
        when(assignmentMapper.selectById(1L)).thenReturn(a(1L));
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 10L));

        UpdateGradeRequest req = new UpdateGradeRequest();
        req.setScore(9);
        req.setComment("很好");
        GradeVO gv = service.updateGrade(5L, req);

        assertThat(gv.score()).isEqualTo(9);
        assertThat(g.getComment()).isEqualTo("很好");
        assertThat(g.getStatus()).isEqualTo(1);
        verify(gradeMapper).updateById(g);
    }

    @Test
    @DisplayName("非本人班级成绩复核 → FORBIDDEN")
    void updateGrade_notOwner_throwsForbidden() {
        AuthContext.set("10", "ROLE_TEACHER");
        Grade g = new Grade();
        g.setId(5L); g.setAssignmentId(1L);
        when(gradeMapper.selectById(5L)).thenReturn(g);
        when(assignmentMapper.selectById(1L)).thenReturn(a(1L));
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 99L)); // 他人班级

        assertThatThrownBy(() -> service.updateGrade(5L, new UpdateGradeRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(403));
    }

    // ── helpers ──

    private static Assignment a(Long id) {
        Assignment x = new Assignment();
        x.setId(id); x.setClassId(1L); x.setTitle("t"); x.setType("homework");
        return x;
    }

    private static AssignmentItem item(Long id, Long assignmentId, Long qid, String type, int score) {
        AssignmentItem i = new AssignmentItem();
        i.setId(id);
        i.setAssignmentId(assignmentId);
        i.setQuestionId(qid);
        i.setItemType(type);
        i.setScore(score);
        return i;
    }

    private static Question q(String type, String answer) {
        Question q = new Question();
        q.setId(type.equals("code") ? 200L : 100L);
        q.setType(type);
        q.setAnswer(answer);
        return q;
    }

    private static Classes c(Long id, Long teacherId) {
        Classes c = new Classes();
        c.setId(id);
        c.setTeacherId(teacherId);
        return c;
    }

    private static SubmitAssignmentRequest reqWith(Long itemId, String submission) {
        SubmitAssignmentRequest r = new SubmitAssignmentRequest();
        SubmitAssignmentRequest.ItemReq ir = new SubmitAssignmentRequest.ItemReq();
        ir.setItemId(itemId);
        ir.setSubmission(submission);
        r.setItems(List.of(ir));
        return r;
    }
}
