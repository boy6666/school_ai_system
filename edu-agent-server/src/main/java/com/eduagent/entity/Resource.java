package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resources")
public class Resource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String type;
    private String difficulty;
    private String description;
    private String content;
    private String fileUrl;
    private String cover;
    private String author;
    private Double rating;
    private Integer views;
    private Integer favorites;
    private String duration;
    private String courseId;
    private String courseName;
    private String tags;
    private String status;
    private Long teacherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
