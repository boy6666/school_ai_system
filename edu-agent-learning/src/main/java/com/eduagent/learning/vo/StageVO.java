package com.eduagent.learning.vo;

import lombok.Data;

import java.util.List;

@Data
public class StageVO {

    private String name;
    private List<TaskVO> tasks;
}
