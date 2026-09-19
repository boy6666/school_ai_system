package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.learning.dto.JudgeRequest;
import com.eduagent.learning.dto.ai.AiResourceRequest;
import com.eduagent.learning.entity.QuizAnswer;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiResultParser.JudgeOutcome;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.QuizAnswerMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 测验作答与判分。选择题本地比对；主观/代码题 Feign→ai mode=judge。
 * AI 不可用时：选择题照常判，主观题 is_correct=null 不阻塞作答链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private static final Set<String> CHOICE_TYPES = Set.of(
            "choice", "single", "single_choice", "multiple", "multiple_choice",
            "true_false", "judgment", "选择题", "单选题", "多选题", "判断题");

    private final QuizAnswerMapper quizAnswerMapper;
    private final StudentProfileMapper profileMapper;
    private final AiServiceClient aiServiceClient;
    private final AiResultParser aiResultParser;

    @Override
    public List<Map<String, Object>> answered(Long studentId, Long resourceId) {
        LambdaQueryWrapper<QuizAnswer> wrapper = new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getStudentId, studentId)
                .orderByDesc(QuizAnswer::getCreateTime);
        if (resourceId != null) {
            wrapper.eq(QuizAnswer::getResourceId, resourceId);
        }
        return quizAnswerMapper.selectList(wrapper).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> wrongQuestions(Long studentId) {
        return quizAnswerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getStudentId, studentId)
                        .eq(QuizAnswer::getIsCorrect, 0)
                        .orderByDesc(QuizAnswer::getCreateTime))
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> wrongQuestionDetail(Long studentId, Long id) {
        QuizAnswer qa = quizAnswerMapper.selectById(id);
        if (qa == null || !qa.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.NOT_FOUND.getCode(), "错题不存在");
        }
        return toMap(qa);
    }

    @Override
    public Map<String, Object> judge(Long studentId, JudgeRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST.getCode(), "题目内容不能为空");
        }
        String type = request.getQuestionType() == null ? "" : request.getQuestionType().toLowerCase(Locale.ROOT);
        boolean choice = CHOICE_TYPES.contains(type)
                || (request.getCorrectAnswer() != null && request.getCorrectAnswer().length() <= 8
                    && type.isBlank());

        Integer isCorrect;
        Integer score;
        String comment;
        String explanation = request.getExplanation();

        if (choice) {
            // 本地规范化比对
            isCorrect = normalize(request.getUserAnswer()).equals(normalize(request.getCorrectAnswer())) ? 1 : 0;
            score = isCorrect;
            comment = isCorrect == 1 ? "回答正确！" : "回答错误，正确答案是 " + request.getCorrectAnswer() + "。";
        } else {
            // 主观/代码题走 AI judge
            JudgeOutcome outcome = null;
            try {
                outcome = aiResultParser.parseJudge(aiServiceClient.generateResource(buildJudgeRequest(studentId, request)));
            } catch (Exception e) {
                log.warn("[Quiz] AI judge 调用失败: {}", e.getMessage());
            }
            if (outcome != null) {
                isCorrect = outcome.isCorrect() ? 1 : 0;
                score = outcome.getScore();
                comment = outcome.getComment() != null ? outcome.getComment()
                        : (isCorrect == 1 ? "回答正确！" : "回答不够完整，建议补充要点。");
                if (outcome.getExplanation() != null) {
                    explanation = outcome.getExplanation();
                }
            } else {
                // 降级：不阻塞作答，标记未判定
                isCorrect = null;
                score = null;
                comment = "AI 判分暂不可用，本次作答已记录，稍后可在错题本查看。";
            }
        }

        QuizAnswer qa = new QuizAnswer();
        qa.setStudentId(studentId);
        qa.setResourceId(request.getResourceId());
        qa.setQuestion(request.getQuestion());
        qa.setQuestionType(request.getQuestionType());
        qa.setUserAnswer(request.getUserAnswer());
        qa.setCorrectAnswer(request.getCorrectAnswer());
        qa.setIsCorrect(isCorrect);
        qa.setExplanation(explanation);
        qa.setCreateTime(LocalDateTime.now());
        quizAnswerMapper.insert(qa);
        log.info("[Quiz] judge 完成 studentId={}, savedId={}, isCorrect={}", studentId, qa.getId(), isCorrect);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("isCorrect", isCorrect);
        out.put("score", score);
        out.put("comment", comment);
        out.put("savedId", qa.getId());
        return out;
    }

    // ---------- 私有方法 ----------

    private AiResourceRequest buildJudgeRequest(Long studentId, JudgeRequest req) {
        StringBuilder prompt = new StringBuilder();
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp != null) {
            prompt.append("学生画像：course=").append(sp.getCourse())
                    .append(", topic=").append(sp.getTopic())
                    .append(", knowledgeBase=").append(sp.getKnowledgeBase()).append("\n\n");
        }
        prompt.append("请判分。返回 JSON：{\"score\": 0或1, \"correct\": true/false, \"comment\": \"评语\", \"explanation\": \"解析\"}\n\n")
                .append("## 题目\n").append(req.getQuestion())
                .append("\n\n## 题型\n").append(req.getQuestionType())
                .append("\n\n## 学生答案\n").append(req.getUserAnswer())
                .append("\n\n## 参考答案\n").append(req.getCorrectAnswer() == null ? "（无）" : req.getCorrectAnswer())
                .append("\n\n评分要点：语义正确、要点覆盖即可给分，不要求逐字一致。纯 JSON 输出。");

        return AiResourceRequest.builder()
                .topic("测验判分")
                .mode("judge")
                .type("judge")
                .extra(Map.of(
                        "studentId", String.valueOf(studentId),
                        "prompt", prompt.toString()))
                .build();
    }

    /** 选择题答案规范化：去空白/标点，多选拆分排序后比较 */
    private String normalize(String answer) {
        if (answer == null) return "";
        String cleaned = answer.replaceAll("[\\s，,、.。;；()（）]", "").toUpperCase(Locale.ROOT);
        if (cleaned.length() > 1) {
            // 多选：逐字符排序，容忍 "AC" / "CA" / "A,C"
            char[] chars = cleaned.toCharArray();
            Arrays.sort(chars);
            return new String(chars);
        }
        return cleaned;
    }

    private Map<String, Object> toMap(QuizAnswer qa) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", qa.getId());
        m.put("resourceId", qa.getResourceId());
        m.put("question", qa.getQuestion());
        m.put("questionType", qa.getQuestionType());
        m.put("userAnswer", qa.getUserAnswer());
        m.put("correctAnswer", qa.getCorrectAnswer());
        m.put("isCorrect", qa.getIsCorrect());
        m.put("explanation", qa.getExplanation());
        m.put("createTime", qa.getCreateTime());
        return m;
    }
}
