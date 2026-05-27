package com.eduagent.controller;

import com.eduagent.agent.AiClient;
import com.eduagent.agent.AiChatResponse;
import com.eduagent.common.Result;
import com.eduagent.dto.TutorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/tutor")
public class TutorController {
    @Autowired
    private AiClient aiClient;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody TutorRequest request) {
        // 获取当前登录学生ID（临时用固定值1L）
        Long studentId = 1L;
        AiChatResponse response = aiClient.chat(studentId.toString(), request.getMessage());
        return Result.success(response);
    }
}
