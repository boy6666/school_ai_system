package com.eduagent.code.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.code.dto.CodeFile;
import com.eduagent.code.dto.CodeSubmitRequest;
import com.eduagent.code.entity.CodeCheckReport;
import com.eduagent.code.entity.CodeSubmission;
import com.eduagent.code.entity.SubmissionStatus;
import com.eduagent.code.mapper.CodeCheckReportMapper;
import com.eduagent.code.mapper.CodeSubmissionMapper;
import com.eduagent.code.service.SubmissionService;
import com.eduagent.code.vo.CodeSubmitReceiptVO;
import com.eduagent.code.vo.CodeSubmitResultVO;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 判分提交受理实现。仅负责落库与查询；判分 Worker 见后续任务（编译/检查/沙箱/判分）。
 * 多文件在数据层单列 source_code 中按约定分隔符拼接，保证内容不丢失、可逆（见 {@link #joinFiles}）。
 */
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    /** 多文件在 source_code 列的拼接分隔符：文件头 + 文件名行，便于判分 Worker 切分还原 */
    private static final String FILE_SEP = "//===== FILE: ";

    private final CodeSubmissionMapper submissionMapper;
    private final CodeCheckReportMapper reportMapper;

    @Override
    @Transactional
    public CodeSubmitReceiptVO submit(CodeSubmitRequest request) {
        CodeSubmission entity = new CodeSubmission();
        entity.setStudentId(resolveStudentId(request));
        entity.setAssignmentId(request.getAssignmentId());
        entity.setAssignmentItemId(request.getAssignmentItemId());
        entity.setLanguage(request.getLanguage());
        entity.setClassName(request.getClassName());
        entity.setExpectedOutput(request.getExpectedOutput());
        entity.setSourceCode(buildSource(request));
        entity.setStatus(SubmissionStatus.PENDING);

        submissionMapper.insert(entity);
        return new CodeSubmitReceiptVO(entity.getId(), SubmissionStatus.PENDING);
    }

    @Override
    public CodeSubmitResultVO getResult(Long submissionId) {
        CodeSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "submissionId=" + submissionId);
        }
        CodeCheckReport report = reportMapper.selectOne(
                new LambdaQueryWrapper<CodeCheckReport>().eq(CodeCheckReport::getSubmissionId, submissionId));

        CodeSubmitResultVO vo = new CodeSubmitResultVO();
        vo.setSubmissionId(sub.getId());
        vo.setStatus(sub.getStatus());
        vo.setStdout(sub.getStdout());
        vo.setRunTimeMs(sub.getRunTimeMs());
        if (report != null) {
            vo.setCompileOk(report.getCompileOk());
            vo.setCheckstyle(report.getCheckstyle());
            vo.setPmd(report.getPmd());
            vo.setAiSuggestion(report.getAiSuggestion());
            vo.setOverallScore(report.getOverallScore());
        }
        return vo;
    }

    /** 体带 studentId 用之，否则回退 AuthContext（学生直连、网关注入身份） */
    private Long resolveStudentId(CodeSubmitRequest request) {
        if (request.getStudentId() != null) {
            return request.getStudentId();
        }
        String userId = AuthContext.getUserId();
        if (userId != null) {
            return Long.valueOf(userId);
        }
        throw new ApiException(ErrorCode.BAD_REQUEST, "缺少 studentId");
    }

    /** 优先 files[]（共识 1）；为空时兼容单文件 sourceCode 形态 */
    private String buildSource(CodeSubmitRequest request) {
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            return joinFiles(request.getFiles());
        }
        if (request.getSourceCode() != null && !request.getSourceCode().isBlank()) {
            return request.getSourceCode();
        }
        throw new ApiException(ErrorCode.BAD_REQUEST, "缺少源码（files 或 sourceCode 至少一项）");
    }

    /**
     * 多文件拼接：每隔文件前写一行 {@code //===== FILE: <name>}，判分 Worker 据此切分还原回多文件。
     */
    private String joinFiles(List<CodeFile> files) {
        return files.stream()
                .map(f -> FILE_SEP + f.getName() + "\n" + f.getSourceCode())
                .collect(Collectors.joining("\n"));
    }
}
