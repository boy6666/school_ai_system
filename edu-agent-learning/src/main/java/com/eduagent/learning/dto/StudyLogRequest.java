package com.eduagent.learning.dto;

import lombok.Data;

/** 学习日志上报（POST /study-log）。module：mindmap/quiz/reading/code。 */
@Data
public class StudyLogRequest {

    private String module;
    private Integer durationSec;
    private Integer chapterId;
    private Integer noteId;
}
