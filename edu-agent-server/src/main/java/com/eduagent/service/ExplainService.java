package com.eduagent.service;

import com.eduagent.dto.ExplainRequest;
import com.eduagent.vo.ExplainResultVO;

public interface ExplainService {
    ExplainResultVO explain(Long studentId, ExplainRequest request);
}
