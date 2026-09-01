package com.eduagent.teacher.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业详情出参（含题目项 + 每题提交/已批数）。
 */
public record AssignmentDetailVO(Long id, Long classId, String title, String type,
                                 String description, LocalDateTime deadline, Integer status,
                                 LocalDateTime createTime, List<ItemDetailVO> items) {

    public record ItemDetailVO(Long itemId, Long questionId, Integer score,
                               QuestionVO question, Integer submittedCount, Integer gradedCount) {
    }
}
