package com.eduagent.service;

import com.eduagent.vo.TutorReplyVO;
import java.util.List;
import java.util.Map;

public interface TutorService {
    TutorReplyVO chat(Long studentId, String message, String sessionId);
    List<Map<String, Object>> getSessions(Long studentId);
    List<TutorReplyVO> getHistory(Long studentId, String sessionId);
}
