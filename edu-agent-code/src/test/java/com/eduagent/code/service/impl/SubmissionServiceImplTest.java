package com.eduagent.code.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.eduagent.code.entity.CodeSubmission;
import com.eduagent.code.entity.SubmissionStatus;
import com.eduagent.code.mapper.CodeCheckReportMapper;
import com.eduagent.code.mapper.CodeSubmissionMapper;
import com.eduagent.code.service.SubmissionService;
import com.eduagent.code.service.worker.JudgeWorker;
import com.eduagent.code.vo.CodeSubmitReceiptVO;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 重新判分受理单测：教师对已出终态的提交触发再判分（首判失败/结果有问题的补救口）。
 * 覆盖 终态重置重判 / 判分进行中409 / 提交不存在404 / 非教师403 四类场景。
 * 事务同步由测试手动 init，afterCommit 显式触发以验证 judgeWorker 入队时机。
 */
class SubmissionServiceImplTest {

    private CodeSubmissionMapper submissionMapper;
    private CodeCheckReportMapper reportMapper;
    private JudgeWorker judgeWorker;
    private SubmissionService service;

    @BeforeEach
    void setUp() {
        submissionMapper = mock(CodeSubmissionMapper.class);
        reportMapper = mock(CodeCheckReportMapper.class);
        judgeWorker = mock(JudgeWorker.class);
        service = new SubmissionServiceImpl(submissionMapper, reportMapper, judgeWorker);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        AuthContext.clear();
    }

    /** 手动触发事务提交回调，验证判分在提交后才入队 */
    private void commitTransaction() {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
    }

    private CodeSubmission submission(long id, int status) {
        CodeSubmission sub = new CodeSubmission();
        sub.setId(id);
        sub.setStudentId(1001L);
        sub.setAssignmentId(5L);
        sub.setAssignmentItemId(12L);
        sub.setLanguage("java");
        sub.setClassName("Main");
        sub.setExpectedOutput("hi");
        sub.setSourceCode("public class Main {}");
        sub.setStatus(status);
        sub.setStdout("old stdout");
        sub.setStderr("old stderr");
        sub.setRunTimeMs(1234);
        when(submissionMapper.selectById(id)).thenReturn(sub);
        return sub;
    }

    @Test
    void teacherRegradeFromTerminalResetsAndRequeues() {
        // 首判失败(FAILED)后教师触发再判分：状态重置、旧输出清空、旧报告删除、提交后重新入队
        CodeSubmission sub = submission(1L, SubmissionStatus.FAILED);
        AuthContext.set("9001", "ROLE_TEACHER");

        CodeSubmitReceiptVO receipt = service.regrade(1L);
        commitTransaction();

        assertThat(receipt.getSubmissionId()).isEqualTo(1L);
        assertThat(receipt.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(sub.getStdout()).isNull();
        assertThat(sub.getStderr()).isNull();
        assertThat(sub.getRunTimeMs()).isNull();
        verify(submissionMapper).updateById(sub);
        verify(reportMapper).delete(any(Wrapper.class));
        verify(judgeWorker).judge(1L);
    }

    @Test
    void regradeRejectedWhileJudgeInProgress() {
        // PENDING/RUNNING 说明首判还没跑完，重复触发会并发双判 → 409
        submission(2L, SubmissionStatus.RUNNING);
        AuthContext.set("9001", "ROLE_TEACHER");

        assertThatThrownBy(() -> service.regrade(2L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(409));
        verify(judgeWorker, never()).judge(any());
    }

    @Test
    void regradeRejectedWhenSubmissionMissing() {
        AuthContext.set("9001", "ROLE_TEACHER");

        assertThatThrownBy(() -> service.regrade(99L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(404));
        verify(judgeWorker, never()).judge(any());
    }

    @Test
    void regradeRequiresTeacherRole() {
        // code 服务无班级归属数据，仅做角色门禁；学生/游客一律 403
        submission(3L, SubmissionStatus.DONE);
        AuthContext.set("2002", "ROLE_STUDENT");

        assertThatThrownBy(() -> service.regrade(3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(403));
        verify(judgeWorker, never()).judge(any());
        verify(submissionMapper, never()).updateById(any(CodeSubmission.class));
    }
}
