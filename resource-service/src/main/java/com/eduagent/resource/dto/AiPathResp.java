package com.eduagent.resource.dto;

import lombok.Data;

@Data
public class AiPathResp {
    private String goal;
    private Object stages;  // 实际是 JSON 数组，用 Object 接收
}
