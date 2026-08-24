package com.eduagent.code.client;

import com.eduagent.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AI 反馈容错（§2.4.5 硬约定）：AI 异常/未部署/超时一律降级为空建议，绝不影响判分主流程。
 */
class AiFeedbackServiceTest {

    private AiServiceClient aiClient;
    private AiFeedbackService aiFeedbackService;

    @BeforeEach
    void setUp() {
        aiClient = Mockito.mock(AiServiceClient.class);
        aiFeedbackService = new AiFeedbackService(aiClient);
    }

    @Test
    void aiDownFallbackToEmpty() {
        when(aiClient.analyze(any())).thenThrow(new RuntimeException("connection refused"));
        assertThat(aiFeedbackService.requestSuggestion(new CodeAnalyzeRequest())).isEmpty();
    }

    @Test
    void aiReturnsNullFallbackToEmpty() {
        when(aiClient.analyze(any())).thenReturn(null);
        assertThat(aiFeedbackService.requestSuggestion(new CodeAnalyzeRequest())).isEmpty();
    }

    @Test
    void aiDataNullFallbackToEmpty() {
        when(aiClient.analyze(any())).thenReturn(Result.success(null));
        assertThat(aiFeedbackService.requestSuggestion(new CodeAnalyzeRequest())).isEmpty();
    }

    @Test
    void useOverallCommentFirst() {
        CodeAnalyzeData data = new CodeAnalyzeData();
        data.setOverallComment("整体良好，规范性待提升");
        data.setSummary("summary");
        when(aiClient.analyze(any())).thenReturn(Result.success(data));
        assertThat(aiFeedbackService.requestSuggestion(new CodeAnalyzeRequest()))
                .isEqualTo("整体良好，规范性待提升");
    }

    @Test
    void fallbackToSummaryWhenNoComment() {
        CodeAnalyzeData data = new CodeAnalyzeData();
        data.setSummary("代码可运行，存在 PMD 隐患");
        when(aiClient.analyze(any())).thenReturn(Result.success(data));
        assertThat(aiFeedbackService.requestSuggestion(new CodeAnalyzeRequest()))
                .isEqualTo("代码可运行，存在 PMD 隐患");
    }
}
