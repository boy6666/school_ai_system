package com.eduagent.code.client;

import com.eduagent.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 反馈容错门面：任何失败（AI 未部署/超时/5xx）都降级为空建议，判分流程不受影响。
 * 返回的 aiSuggestion 只作参考写入报告，不参与判分（§2.4.6 "AI(参考)"）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiServiceClient aiServiceClient;

    public String requestSuggestion(CodeAnalyzeRequest request) {
        try {
            Result<CodeAnalyzeData> resp = aiServiceClient.analyze(request);
            if (resp != null && resp.getData() != null) {
                String comment = resp.getData().getOverallComment();
                if (StringUtils.hasText(comment)) {
                    return comment;
                }
                String summary = resp.getData().getSummary();
                if (StringUtils.hasText(summary)) {
                    return summary;
                }
            }
        } catch (Exception e) {
            log.warn("AI 反馈不可用，降级为空建议: {}", e.toString());
        }
        return "";
    }
}
