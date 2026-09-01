package com.eduagent.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.result.Result;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.SubmitAssignmentRequest;
import com.eduagent.teacher.dto.UpdateGradeRequest;
import com.eduagent.teacher.entity.Assignment;
import com.eduagent.teacher.entity.AssignmentItem;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.entity.Grade;
import com.eduagent.teacher.entity.Question;
import com.eduagent.teacher.feign.CodeServiceClient;
import com.eduagent.teacher.feign.CodeSubmissionRequest;
import com.eduagent.teacher.feign.CodeSubmitReceiptVO;
import com.eduagent.teacher.mapper.AssignmentItemMapper;
import com.eduagent.teacher.mapper.AssignmentMapper;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.ClassStudentMapper;
import com.eduagent.teacher.mapper.GradeMapper;
import com.eduagent.teacher.mapper.QuestionMapper;
import com.eduagent.teacher.service.GradeService;
import com.eduagent.teacher.vo.GradeDetailVO;
import com.eduagent.teacher.vo.GradeVO;
import com.eduagent.teacher.vo.StudentAssignmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentItemMapper itemMapper;
    private final QuestionMapper questionMapper;
    private final GradeMapper gradeMapper;
    private final ClassesMapper classesMapper;
    private final ClassStudentMapper classStudentMapper;
    private final CodeServiceClient codeClient;

    @Override
    @Transactional
    public List<GradeVO> submit(Long assignmentId, SubmitAssignmentRequest request) {
        Long studentId = currentUserId();
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作业不存在");
        }

        List<GradeVO> result = new ArrayList<>();
        for (SubmitAssignmentRequest.ItemReq itemReq : request.getItems()) {
            Grade grade = upsert(itemReq, assignmentId, studentId);
            result.add(toGradeVO(grade));
        }
        return result;
    }

    /** 单题（item）处理：写入/更新 grades；choice/blank 本地判分，code 走异步受理 */
    private Grade upsert(SubmitAssignmentRequest.ItemReq itemReq, Long assignmentId, Long studentId) {
        AssignmentItem item = itemMapper.selectById(itemReq.getItemId());
        if (item == null || !item.getAssignmentId().equals(assignmentId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "题目项不属于该作业: " + itemReq.getItemId());
        }
        Question question = questionMapper.selectById(item.getQuestionId());

        Grade grade = gradeMapper.selectByStuItem(assignmentId, studentId, item.getId());
        boolean isNew = grade == null;
        if (isNew) {
            grade = new Grade();
            grade.setAssignmentId(assignmentId);
            grade.setStudentId(studentId);
            grade.setItemId(item.getId());
            grade.setItemType(item.getItemType());
        }
        grade.setSubmission(itemReq.getSubmission());
        grade.setLanguage(itemReq.getLanguage());

        boolean local;
        if ("code".equalsIgnoreCase(item.getItemType())) {
            // 异步两段式：先落"待批"，再由 assignment.graded 事件回填
            grade.setStatus(0);
            grade.setGradedAt(null);
            local = false;
        } else {
            boolean correct = question != null && question.getAnswer() != null
                    && question.getAnswer().trim().equals(itemReq.getSubmission() == null
                            ? "" : itemReq.getSubmission().trim());
            grade.setScore(correct ? item.getScore() : 0);
            grade.setStatus(1);
            grade.setGradedAt(LocalDateTime.now());
            local = true;
        }

        if (!local) {
            // code 题：先受理拿回执（submissionId 是教师重判入口），失败不阻断，事件回填兜底
            String language = itemReq.getLanguage() == null ? "java" : itemReq.getLanguage();
            CodeSubmissionRequest req = CodeSubmissionRequest.builder()
                    .studentId(studentId)
                    .assignmentId(assignmentId)
                    .assignmentItemId(item.getId())
                    .language(language)
                    .sourceCode(itemReq.getSubmission())
                    .expectedOutput(question == null ? null : question.getAnswer())
                    .className(null)
                    .build();
            try {
                Result<CodeSubmitReceiptVO> receipt = codeClient.submit(req);
                if (receipt != null && receipt.getData() != null
                        && receipt.getData().submissionId() != null) {
                    grade.setSubmissionId(receipt.getData().submissionId());
                }
            } catch (Exception e) {
                log.warn("提交 code 判分受理失败 assignmentId={} itemId={}: {}",
                        assignmentId, item.getId(), e.getMessage());
            }
        }

        if (isNew) {
            gradeMapper.insert(grade);
        } else {
            gradeMapper.updateById(grade);
        }
        return grade;
    }

    @Override
    public List<GradeVO> listGrades(Long assignmentId, Long studentId) {
        requireAssignmentOwner(assignmentId, "查询成绩");
        LambdaQueryWrapper<Grade> w = new LambdaQueryWrapper<Grade>()
                .eq(Grade::getAssignmentId, assignmentId)
                .eq(studentId != null, Grade::getStudentId, studentId)
                .orderByAsc(Grade::getStudentId).orderByAsc(Grade::getItemId);
        return gradeMapper.selectList(w).stream().map(this::toGradeVO).collect(Collectors.toList());
    }

    @Override
    public GradeDetailVO getGrade(Long gradeId) {
        Grade grade = requireGrade(gradeId);
        // 学生只能看自己，教师/管理员可看
        requireViewer(grade);
        return new GradeDetailVO(grade.getId(), grade.getAssignmentId(), grade.getStudentId(),
                grade.getItemId(), grade.getItemType(), grade.getLanguage(), grade.getSubmissionId(),
                grade.getSubmission(), grade.getScore(), grade.getStatus(), grade.getGradedAt(),
                grade.getRunResult(), grade.getStaticReport(), grade.getAiReport(), grade.getComment());
    }

    @Override
    public GradeVO updateGrade(Long gradeId, UpdateGradeRequest request) {
        Grade grade = requireGrade(gradeId);
        Assignment a = requireAssignmentOwner(grade.getAssignmentId(), "复核成绩");
        if (request.getScore() != null) {
            grade.setScore(request.getScore());
        }
        if (request.getComment() != null) {
            grade.setComment(request.getComment());
        }
        if (request.getAiReportOverride() != null) {
            grade.setAiReport(request.getAiReportOverride());
        }
        grade.setStatus(1);
        grade.setGradedAt(LocalDateTime.now());
        gradeMapper.updateById(grade);
        return toGradeVO(grade);
    }

    @Override
    public List<StudentAssignmentVO> studentAssignments(Long studentId) {
        Long userId = currentUserId();
        boolean isTeacher = isTeacher();
        if (!isTeacher && !userId.equals(studentId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权查看他人作业");
        }
        List<Long> classIds = classStudentMapper.selectClassIds(studentId);
        Set<Long> classSet = new HashSet<>(classIds);
        List<Assignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>().orderByDesc(Assignment::getId));

        List<StudentAssignmentVO> result = new ArrayList<>();
        for (Assignment a : assignments) {
            if (!classSet.contains(a.getClassId())) {
                continue;
            }
            List<Grade> grades = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                    .eq(Grade::getAssignmentId, a.getId())
                    .eq(Grade::getStudentId, studentId));
            int myScore = grades.stream().filter(g -> g.getStatus() != null && g.getStatus() == 1)
                    .mapToInt(g -> g.getScore() == null ? 0 : g.getScore()).sum();
            int totalScore = itemMapper.selectList(new LambdaQueryWrapper<AssignmentItem>()
                            .eq(AssignmentItem::getAssignmentId, a.getId()))
                    .stream().mapToInt(i -> i.getScore() == null ? 0 : i.getScore()).sum();
            LocalDateTime submittedAt = grades.stream()
                    .map(Grade::getGradedAt).filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo).orElse(null);
            result.add(new StudentAssignmentVO(a.getId(), a.getClassId(), a.getTitle(), a.getType(),
                    a.getDeadline(), a.getStatus(), myScore, totalScore, submittedAt));
        }
        return result;
    }

    // ── helpers ──

    private Grade requireGrade(Long gradeId) {
        Grade g = gradeMapper.selectById(gradeId);
        if (g == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "成绩不存在");
        }
        return g;
    }

    private void requireViewer(Grade grade) {
        if (isTeacher()) {
            return;
        }
        // 学生仅限本人
        if (!currentUserId().equals(grade.getStudentId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权查看他人成绩");
        }
    }

    private Assignment requireAssignmentOwner(Long assignmentId, String action) {
        Assignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作业不存在");
        }
        if (!isTeacher()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "需要教师权限: " + action);
        }
        Classes c = classesMapper.selectById(a.getClassId());
        if (c != null && !c.getTeacherId().equals(currentUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权操作他人班级的作业: " + action);
        }
        return a;
    }

    private boolean isTeacher() {
        String roles = AuthContext.getRoles();
        if (roles == null) {
            return false;
        }
        return roles.contains(ServiceConstants.ROLE_TEACHER)
                || roles.contains(ServiceConstants.ROLE_ADMIN);
    }

    private Long currentUserId() {
        String uid = AuthContext.getUserId();
        if (uid == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        return Long.valueOf(uid);
    }

    private GradeVO toGradeVO(Grade g) {
        return new GradeVO(g.getId(), g.getAssignmentId(), g.getStudentId(), g.getItemId(),
                g.getItemType(), g.getLanguage(), g.getSubmissionId(), g.getSubmission(),
                g.getScore(), g.getStatus(), g.getGradedAt(), g.getAiReport() != null);
    }
}
