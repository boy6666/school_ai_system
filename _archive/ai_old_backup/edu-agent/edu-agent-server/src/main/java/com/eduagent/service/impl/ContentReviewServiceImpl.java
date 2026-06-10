package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.entity.Conversation;
import com.eduagent.mapper.ConversationMapper;
import com.eduagent.service.ContentReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentReviewServiceImpl implements ContentReviewService {

    private final ConversationMapper conversationMapper;

    @Override
    public Page<Conversation> listConversations(int page, int pageSize, String keyword, String intent) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Conversation::getQuestion, keyword).or().like(Conversation::getAnswer, keyword);
        }
        if (intent != null && !intent.isEmpty()) {
            wrapper.eq(Conversation::getIntent, intent);
        }
        wrapper.orderByDesc(Conversation::getCreateTime);
        return conversationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void flagConversation(Long id, String flag) {
        Conversation conv = conversationMapper.selectById(id);
        if (conv != null) {
            conv.setResourceDir(flag);  // 复用 resourceDir 字段存审核标记
            conversationMapper.updateById(conv);
        }
    }

    @Override
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", conversationMapper.selectCount(null));
        
        LambdaQueryWrapper<Conversation> today = new LambdaQueryWrapper<>();
        today.apply("DATE(create_time) = CURDATE()");
        stats.put("today", conversationMapper.selectCount(today));
        
        LambdaQueryWrapper<Conversation> explain = new LambdaQueryWrapper<>();
        explain.eq(Conversation::getIntent, "explain");
        stats.put("byExplain", conversationMapper.selectCount(explain));
        
        LambdaQueryWrapper<Conversation> quiz = new LambdaQueryWrapper<>();
        quiz.eq(Conversation::getIntent, "quiz");
        stats.put("byQuiz", conversationMapper.selectCount(quiz));
        
        return stats;
    }
}
