package com.eduagent.learning.service;

import com.eduagent.learning.vo.AiChatResult;
import com.eduagent.learning.vo.TutorReplyVO;

import java.util.List;
import java.util.Map;

public interface TutorService {

    /** 智能辅导对话：调 AI → 画像回写 → 对话留存 */
    TutorReplyVO chat(Long studentId, String message, String sessionId);

    /** 引导对话：调 AI → 画像合并（引导完成置 profile_complete）→ 对话留存 */
    AiChatResult onboardChat(Long studentId, String message, String sessionId, Map<String, Object> collectedProfile);

    /** 会话列表（按 sessionId 去重） */
    List<Map<String, Object>> getSessions(Long studentId);

    /** 对话历史（sessionId 可空=全部） */
    List<TutorReplyVO> getHistory(Long studentId, String sessionId);
}
