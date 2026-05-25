package com.eduagent.service;

import com.eduagent.vo.TutorReplyVO;
import java.util.List;

public interface TutorService {
    TutorReplyVO chat(Long studentId, String message, String sessionId);
    List<TutorReplyVO> getHistory(Long studentId, String sessionId);
}
