package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.entity.Conversation;
import java.util.Map;

public interface ContentReviewService {
    Page<Conversation> listConversations(int page, int pageSize, String keyword, String intent);
    void flagConversation(Long id, String flag);
    Map<String, Object> getReviewStats();
}
