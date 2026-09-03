package com.eduagent.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("learning_resources")
public class Resource {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime updateTime;
}
