package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("java_notes")
public class JavaNotes {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String category;
    private String title;
    private String filename;
    private String content;
    private LocalDateTime createdAt;
}
