package com.eduagent.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 学习日志上报（POST /study-log）。module：mindmap/quiz/reading/code。 */
@Data
public class StudyLogRequest {

    @NotBlank(message = "module 不能为空")
    private String module;

    @NotNull(message = "durationSec 不能为空")
    @Positive(message = "durationSec 必须为正整数")
    private Integer durationSec;
    private Integer chapterId;
    private Integer noteId;
}
