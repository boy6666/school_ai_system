package com.eduagent.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.AiResourceRequest;
import com.eduagent.teacher.dto.CreateQuestionRequest;
import com.eduagent.teacher.dto.QuestionGenerateRequest;
import com.eduagent.teacher.feign.AiServiceClient;
import com.eduagent.teacher.entity.Question;
import com.eduagent.teacher.mapper.QuestionMapper;
import com.eduagent.teacher.service.QuestionService;
import com.eduagent.teacher.vo.QuestionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;
    private final AiServiceClient aiClient;

    @Override
    public QuestionVO create(CreateQuestionRequest request) {
        Question q = new Question();
        applyFields(q, request);
        q.setCreatorId(currentUserId());
        questionMapper.insert(q);
        return toVO(q);
    }

    @Override
    public List<QuestionVO> list(String chapter, String topic, String type, String difficulty) {
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<Question>()
                .eq(StringUtils.hasText(chapter), Question::getChapter, chapter)
                .eq(StringUtils.hasText(topic), Question::getTopic, topic)
                .eq(StringUtils.hasText(type), Question::getType, type)
                .eq(StringUtils.hasText(difficulty), Question::getDifficulty, difficulty)
                .orderByDesc(Question::getId);
        return questionMapper.selectList(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public QuestionVO get(Long id) {
        return toVO(require(id));
    }

    @Override
    public QuestionVO update(Long id, CreateQuestionRequest request) {
        Question q = require(id);
        applyFields(q, request);
        questionMapper.updateById(q);
        return toVO(q);
    }

    @Override
    public void delete(Long id) {
        require(id);
        questionMapper.deleteById(id);
    }

    @Override
    public List<QuestionVO> generate(QuestionGenerateRequest request) {
        AiResourceRequest req = AiResourceRequest.builder()
                .mode("quiz")
                .chapter(request.getChapter())
                .topic(request.getTopic())
                .type(request.getType())
                .difficulty(request.getDifficulty())
                .count(request.getCount())
                .build();
        try {
            Map<String, Object> data = aiClient.generate(req).getData();
            if (data == null || !(data.get("items") instanceof List<?>)) {
                return Collections.emptyList();
            }
            List<QuestionVO> drafts = new ArrayList<>();
            for (Object o : (List<?>) data.get("items")) {
                if (!(o instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> m = (Map<?, ?>) o;
                List<String> options = m.get("options") instanceof List<?> opts
                        ? opts.stream().map(String::valueOf).toList() : Collections.emptyList();
                drafts.add(new QuestionVO(null,
                        str(m, "type", request.getType()),
                        str(m, "chapter", request.getChapter()),
                        str(m, "topic", request.getTopic()),
                        str(m, "content", ""),
                        options,
                        str(m, "answer", null),
                        str(m, "explanation", null),
                        str(m, "difficulty", request.getDifficulty()),
                        null, null));
            }
            return drafts;
        } catch (Exception e) {
            log.warn("AI 出题失败: {}", e.getMessage());
            throw new ApiException(ErrorCode.SYSTEM_ERROR, "AI 出题服务暂不可用");
        }
    }

    private String str(Map<?, ?> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private Long currentUserId() {
        String uid = AuthContext.getUserId();
        if (uid == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        return Long.valueOf(uid);
    }

    private Question require(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return q;
    }

    /** 把请求字段写入实体；options 序列化为 JSON 字符串入库 */
    private void applyFields(Question q, CreateQuestionRequest req) {
        q.setType(req.getType());
        q.setChapter(req.getChapter());
        q.setTopic(req.getTopic());
        q.setContent(req.getContent());
        q.setOptions(req.getOptions() == null ? null : writeOptions(req.getOptions()));
        q.setAnswer(req.getAnswer());
        q.setExplanation(req.getExplanation());
        q.setDifficulty(req.getDifficulty());
    }

    private String writeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "选项 JSON 序列化失败");
        }
    }

    private QuestionVO toVO(Question q) {
        List<String> options = parseOptions(q.getOptions());
        return new QuestionVO(q.getId(), q.getType(), q.getChapter(), q.getTopic(),
                q.getContent(), options, q.getAnswer(), q.getExplanation(),
                q.getDifficulty(), q.getCreatorId(), q.getCreateTime());
    }

    private List<String> parseOptions(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析 options 失败 id={}: {}", json, e.getMessage());
            return Collections.emptyList();
        }
    }
}
