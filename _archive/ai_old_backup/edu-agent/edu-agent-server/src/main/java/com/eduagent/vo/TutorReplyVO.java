package com.eduagent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorReplyVO {
    private String answer;
    private String intent;
    private String routeReason;
    private String evaluation;
    private String resourceDir;
}
