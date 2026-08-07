package com.eduagent.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.CreateAssignmentRequest;
import com.eduagent.teacher.dto.UpdateAssignmentRequest;
import com.eduagent.teacher.entity.Assignment;
import com.eduagent.teacher.entity.AssignmentItem;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.entity.Grade;
import com.eduagent.teacher.entity.Question;
import com.eduagent.teacher.mapper.AssignmentItemMapper;
import com.eduagent.teacher.mapper.AssignmentMapper;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.GradeMapper;
import com.eduagent.teacher.mapper.QuestionMapper;
import com.eduagent.teacher.mq.AssignmentPublishedEvent;
import com.eduagent.teacher.mq.AssignmentPublishedPublisher;
import com.eduagent.teacher.service.AssignmentService;
import com.eduagent.teacher.vo.AssignmentDetailVO;
import com.eduagent.teacher.vo.AssignmentVO;
import com.eduagent.teacher.vo.QuestionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentItemMapper itemMapper;
    private final QuestionMapper questionMapper;
    private final ClassesMapper classesMapper;
    private final GradeMapper gradeMapper;
    private final AssignmentPublishedPublisher publisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AssignmentVO create(CreateAssignmentRequest request) {
        Long userId = currentUserId();
        requireOwnClass(request.getClassId(), userId);

        Assignment a = new Assignment();
        a.setClassId(request.getClassId());
        a.setTitle(request.getTitle());
        a.setType(request.getType());
        a.setDescription(request.getDescription());
        a.setDeadline(request.getDeadline());
        a.setStatus(0); // 草稿
        a.setCreatorId(userId);
        assignmentMapper.insert(a);

        int order = 0;
        for (CreateAssignmentRequest.ItemReq item : request.getItems()) {
            requireQuestion(item.getQuestionId());
            AssignmentItem ai = new AssignmentItem();
            ai.setAssignmentId(a.getId());
            ai.setQuestionId(item.getQuestionId());
            ai.setItemType(questionMapper.selectById(item.getQuestionId()).getType());
            ai.setScore(item.getScore());
            ai.setOrderNum(order++);
            itemMapper.insert(ai);
        }
        return toVO(a);
    }

    @Override
    public List<AssignmentVO> list(Long classId) {
        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getCreatorId, currentUserId())
                .eq(classId != null, Assignment::getClassId, classId)
                .orderByDesc(Assignment::getId);
        return assignmentMapper.selectList(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AssignmentDetailVO getDetail(Long id) {
        Assignment a = requireOwned(id);
        List<AssignmentItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<AssignmentItem>()
                        .eq(AssignmentItem::getAssignmentId, id)
                        .orderByAsc(AssignmentItem::getOrderNum));
        List<AssignmentDetailVO.ItemDetailVO> itemVOs = new ArrayList<>();
        for (AssignmentItem item : items) {
            Question q = questionMapper.selectById(item.getQuestionId());
            long submitted = gradeCount(id, item.getId(), null);
            long graded = gradeCount(id, item.getId(), 1);
            itemVOs.add(new AssignmentDetailVO.ItemDetailVO(item.getId(), item.getQuestionId(),
                    item.getScore(), q == null ? null : toQuestionVO(q),
                    (int) submitted, (int) graded));
        }
        return new AssignmentDetailVO(a.getId(), a.getClassId(), a.getTitle(), a.getType(),
                a.getDescription(), a.getDeadline(), a.getStatus(), a.getCreateTime(), itemVOs);
    }

    @Override
    public AssignmentVO update(Long id, UpdateAssignmentRequest request) {
        Assignment a = requireOwned(id);
        if (request.getTitle() != null) {
            a.setTitle(request.getTitle());
        }
        if (request.getDeadline() != null) {
            a.setDeadline(request.getDeadline());
        }
        if (request.getStatus() != null) {
            a.setStatus(Integer.valueOf(request.getStatus()));
        }
        assignmentMapper.updateById(a);
        return toVO(a);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireOwned(id);
        assignmentMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<AssignmentItem>()
                .eq(AssignmentItem::getAssignmentId, id));
    }

    @Override
    public AssignmentVO addItem(Long id, CreateAssignmentRequest.ItemReq item) {
        Assignment a = requireOwned(id);
        requireQuestion(item.getQuestionId());
        AssignmentItem ai = new AssignmentItem();
        ai.setAssignmentId(id);
        ai.setQuestionId(item.getQuestionId());
        ai.setItemType(questionMapper.selectById(item.getQuestionId()).getType());
        ai.setScore(item.getScore());
        ai.setOrderNum(countItems(id));
        itemMapper.insert(ai);
        return toVO(a);
    }

    @Override
    public AssignmentVO publish(Long id) {
        Assignment a = requireOwned(id);
        a.setStatus(1);
        assignmentMapper.updateById(a);
        AssignmentPublishedEvent e = new AssignmentPublishedEvent();
        e.setAssignmentId(a.getId());
        e.setClassId(a.getClassId());
        e.setTitle(a.getTitle());
        e.setType(a.getType());
        e.setDeadline(a.getDeadline());
        publisher.publish(e);
        return toVO(a);
    }

    // ── helpers ──

    private Assignment requireOwned(Long id) {
        Assignment a = assignmentMapper.selectById(id);
        if (a == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作业不存在");
        }
        requireOwnClass(a.getClassId(), currentUserId());
        return a;
    }

    private void requireOwnClass(Long classId, Long userId) {
        Classes c = classesMapper.selectById(classId);
        if (c == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "班级不存在");
        }
        if (!c.getTeacherId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权操作他人班级的作业");
        }
    }

    private void requireQuestion(Long questionId) {
        if (questionMapper.selectById(questionId) == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "题目不存在: " + questionId);
        }
    }

    private int countItems(Long assignmentId) {
        return Math.toIntExact(itemMapper.selectCount(
                new LambdaQueryWrapper<AssignmentItem>()
                        .eq(AssignmentItem::getAssignmentId, assignmentId)));
    }

    private long gradeCount(Long assignmentId, Long itemId, Integer status) {
        return gradeMapper.selectCount(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getAssignmentId, assignmentId)
                .eq(Grade::getItemId, itemId)
                .eq(status != null, Grade::getStatus, status));
    }

    private AssignmentVO toVO(Assignment a) {
        List<AssignmentItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<AssignmentItem>()
                        .eq(AssignmentItem::getAssignmentId, a.getId()));
        int totalScore = items.stream().mapToInt(x -> x.getScore() == null ? 0 : x.getScore()).sum();
        return new AssignmentVO(a.getId(), a.getClassId(), a.getTitle(), a.getType(),
                a.getDeadline(), a.getStatus(), a.getCreateTime(), items.size(), totalScore);
    }

    private QuestionVO toQuestionVO(Question q) {
        List<String> options = Collections.emptyList();
        if (StringUtils.hasText(q.getOptions())) {
            try {
                options = objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                log.warn("解析 options 失败 qid={}: {}", q.getId(), e.getMessage());
            }
        }
        return new QuestionVO(q.getId(), q.getType(), q.getChapter(), q.getTopic(),
                q.getContent(), options, q.getAnswer(), q.getExplanation(),
                q.getDifficulty(), q.getCreatorId(), q.getCreateTime());
    }

    private Long currentUserId() {
        String uid = AuthContext.getUserId();
        if (uid == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        return Long.valueOf(uid);
    }
}
