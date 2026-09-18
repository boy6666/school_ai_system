package com.eduagent.learning.feign;

import com.eduagent.learning.vo.AiChatResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI 返回宽容解析器单测：Result 包装 / 裸 JSON / markdown 围栏 三种形态。 */
class AiResultParserTest {

    private AiResultParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResultParser(new ObjectMapper());
    }

    @Test
    void parseSuggestions_fromWrappedResult() {
        String raw = "{\"code\":0,\"message\":\"ok\",\"data\":{\"suggestions\":[\"建议1\",\"建议2\"]}}";
        assertEquals(List.of("建议1", "建议2"), parser.parseSuggestions(raw));
    }

    @Test
    void parseSuggestions_fromBarePayload() {
        String raw = "{\"suggestions\":[\"A\"]}";
        assertEquals(List.of("A"), parser.parseSuggestions(raw));
    }

    @Test
    void parseSuggestions_emptyOnGarbage() {
        assertTrue(parser.parseSuggestions("not-json").isEmpty());
        assertTrue(parser.parseSuggestions(null).isEmpty());
    }

    @Test
    void parseJudge_wrappedCamelCase() {
        String raw = "{\"code\":0,\"data\":{\"score\":1,\"correct\":true,\"comment\":\"要点覆盖\",\"explanation\":\"略\"}}";
        AiResultParser.JudgeOutcome o = parser.parseJudge(raw);
        assertNotNull(o);
        assertTrue(o.isCorrect());
        assertEquals(1, o.getScore());
        assertEquals("要点覆盖", o.getComment());
    }

    @Test
    void parseJudge_incorrectWithoutExplanation() {
        String raw = "{\"correct\":false,\"score\":0}";
        AiResultParser.JudgeOutcome o = parser.parseJudge(raw);
        assertNotNull(o);
        assertEquals(0, o.getScore());
        assertNull(o.getExplanation());
    }

    @Test
    void parseJudge_nullOnUnparseable() {
        assertNull(parser.parseJudge("AI 服务熔断"));
    }

    @Test
    void parseChatResult_snakeCaseAlias() {
        String raw = "{\"code\":0,\"data\":{\"final_answer\":\"多态是指……\",\"intent\":\"explain\","
                + "\"profile_complete\":true,\"profile\":{\"topic\":\"面向对象\"}}}";
        AiChatResult r = parser.parseChatResult(raw);
        assertNotNull(r);
        assertEquals("多态是指……", r.getFinalAnswer());
        assertEquals(Boolean.TRUE, r.getProfileComplete());
        assertEquals(Map.of("topic", "面向对象"), r.getProfile());
    }

    @Test
    void parsePathData_joinsArraySuggestions() {
        String raw = "{\"code\":0,\"data\":{\"goal\":\"g\",\"suggestions\":[\"a\",\"b\"],"
                + "\"stages\":[{\"name\":\"今日计划\",\"tasks\":[{\"title\":\"t\",\"duration\":30}]}]}}";
        Map<String, Object> data = parser.parsePathData(raw);
        assertNotNull(data);
        assertEquals("g", data.get("goal"));
        assertEquals("a；b", data.get("suggestions"));
    }

    @Test
    void parseEmbeddedJson_stripsFences() {
        String text = "好的，结果如下：\n```json\n{\"summary\":\"ok\",\"score\":88}\n```";
        Map<String, Object> json = parser.parseEmbeddedJson(text);
        assertNotNull(json);
        assertEquals("ok", json.get("summary"));
        assertEquals(88, json.get("score"));
    }

    @Test
    void parseData_nullWhenCodeNonZeroWithNullData() {
        assertNull(parser.parseData("{\"code\":500,\"message\":\"err\",\"data\":null}"));
    }
}
