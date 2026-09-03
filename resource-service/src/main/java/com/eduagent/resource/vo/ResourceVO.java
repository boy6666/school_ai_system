package com.eduagent.resource.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResourceVO {
    private Long id;
    private Long userId;
    private String title;
    private String type;
    private String difficulty;
    private String chapter;
    private String chapterId;
    private String courseName;
    private String description;
    private String content;
    private String status;
    private String errorMsg;
    private Double rating;
    private Integer views;
    private Integer favorites;
    private LocalDateTime createTime;
}
