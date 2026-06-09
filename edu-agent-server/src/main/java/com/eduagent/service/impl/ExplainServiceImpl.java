package com.eduagent.service.impl;

import com.eduagent.agent.AiClient;
import com.eduagent.dto.ExplainRequest;
import com.eduagent.entity.QuizAnswer;
import com.eduagent.entity.StudentProfile;
import com.eduagent.mapper.QuizAnswerMapper;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.service.ExplainService;
import com.eduagent.vo.ExplainResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExplainServiceImpl implements ExplainService {

    private final AiClient aiClient;
    private final QuizAnswerMapper quizAnswerMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ExplainResultVO explain(Long studentId, ExplainRequest request) {
        log.info("讲解请求: studentId={}, questionType={}, isCorrect={}",
                studentId, request.getQuestionType(), request.getIsCorrect());

        // 1. 读取学生画像
        Map<String, Object> profileMap = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
            if (sp != null) {
                profileMap.put("course", sp.getCourse());
                profileMap.put("topic", sp.getTopic());
                profileMap.put("knowledge_base", sp.getKnowledgeBase());
                profileMap.put("weaknesses", sp.getWeaknesses());
                profileMap.put("pace", sp.getPace());
                profileMap.put("resource_preference", sp.getResourcePreference());
                profileMap.put("last_score", sp.getLastScore());
            }
        } catch (Exception e) {
            log.warn("读取画像失败: {}", e.getMessage());
        }

        // 2. 构建讲解 prompt
        String prompt = buildExplainPrompt(request, profileMap);
        log.info("讲解 prompt 长度: {}", prompt.length());

        // 3. 调 AI 生成讲解
        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("chapter", "Java 程序设计");
        aiBody.put("topic", "题目讲解");
        aiBody.put("resourceType", "explain");
        aiBody.put("level", "basic");
        aiBody.put("prompt", prompt);

        String aiResult;
        try {
            aiResult = aiClient.post("/resource/generate", aiBody);
            // 解析返回的 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(aiResult, Map.class);
            String explanation = (String) resultMap.getOrDefault("content", aiResult);
            if (explanation == null || explanation.isBlank()) {
                explanation = aiResult;
            }

            // 4. 存入 quiz_answer 表
            QuizAnswer qa = new QuizAnswer();
            qa.setStudentId(studentId);
            qa.setResourceId(request.getResourceId());
            qa.setQuestion(request.getQuestion());
            qa.setQuestionType(request.getQuestionType());
            qa.setUserAnswer(request.getUserAnswer());
            qa.setCorrectAnswer(request.getCorrectAnswer());
            qa.setIsCorrect(Boolean.TRUE.equals(request.getIsCorrect()) ? 1 : 0);
            qa.setExplanation(explanation);
            qa.setCreateTime(LocalDateTime.now());
            quizAnswerMapper.insert(qa);
            log.info("讲解记录已存入 quiz_answer: id={}", qa.getId());

            return ExplainResultVO.builder()
                    .correct(request.getIsCorrect())
                    .explanation(explanation)
                    .build();

        } catch (Exception e) {
            log.error("AI 讲解生成失败", e);
            // fallback 讲解
            String fallback = "这道题" + (Boolean.TRUE.equals(request.getIsCorrect()) ? "你做对了！" : "你做错了。")
                    + "正确答案是 " + request.getCorrectAnswer() + "。"
                    + "建议回顾相关知识点，加深理解。";
            return ExplainResultVO.builder()
                    .correct(request.getIsCorrect())
                    .explanation(fallback)
                    .build();
        }
    }

    private String buildExplainPrompt(ExplainRequest req, Map<String, Object> profile) {
        StringBuilder sb = new StringBuilder();

        // 画像信息
        sb.append("学生画像信息：\n");
        if (profile.get("course") != null) sb.append("- 课程：").append(profile.get("course")).append("\n");
        if (profile.get("topic") != null) sb.append("- 当前主题：").append(profile.get("topic")).append("\n");
        if (profile.get("pace") != null) sb.append("- 学习节奏：").append(profile.get("pace")).append("\n");
        if (profile.get("weaknesses") != null) sb.append("- 薄弱点：").append(profile.get("weaknesses")).append("\n");
        if (profile.get("knowledge_base") != null) sb.append("- 知识基础：").append(profile.get("knowledge_base")).append("\n");
        if (profile.get("resource_preference") != null) sb.append("- 资源偏好：").append(profile.get("resource_preference")).append("\n");
        sb.append("\n");

        // 题目信息
        sb.append("## 题目\n").append(req.getQuestion()).append("\n\n");
        sb.append("## 用户答案\n").append(req.getUserAnswer()).append("\n\n");
        sb.append("## 正确答案\n").append(req.getCorrectAnswer()).append("\n\n");

        if (Boolean.TRUE.equals(req.getIsCorrect())) {
            sb.append("## 答题结果：正确\n");
            sb.append("用户答对了。请充分肯定用户的回答，然后：\n");
            sb.append("1. 简要解释这道题的核心知识点\n");
            sb.append("2. 结合学生的薄弱点，给出拓展延伸\n");
            sb.append("3. 提出一个进阶问题引导学生继续思考\n");
        } else {
            sb.append("## 答题结果：错误\n");
            sb.append("用户答错了。请：\n");
            sb.append("1. 先指出用户的错误点\n");
            sb.append("2. 对比正确答案，解释为什么错（常见误区）\n");
            sb.append("3. 结合学生薄弱点重新讲解相关知识点\n");
            sb.append("4. 给出学习建议\n");
        }

        sb.append("\n请用通俗易懂的语言讲解，结合学生画像中的薄弱点和知识基础进行针对性讲解。");
        sb.append("字数控制在 300-500 字。");

        return sb.toString();
    }
}
