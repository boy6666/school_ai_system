package com.eduagent.resource.dto;

import lombok.Data;

@Data
public class ResourceGenerateReq {
    private Long userId;
    private String chapter;
    private String chapterName;
    private String topic;
    private String type;
    private String difficulty;
    private boolean force;
    private String chapterId;   // 新增字段，对应数据库 chapter_id
}
