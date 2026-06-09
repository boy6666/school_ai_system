package com.eduagent.dto;

import lombok.Data;

@Data
public class ExplainRequest {
    private Long resourceId;
    private String question;
    private String questionType;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
}
