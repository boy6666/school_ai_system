package com.eduagent.resource.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KbCorpusVO {
    private Long id;
    private String content;
    private String source;
    private String type;
    private String status;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
