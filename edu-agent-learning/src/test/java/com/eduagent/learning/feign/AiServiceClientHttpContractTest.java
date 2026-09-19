package com.eduagent.learning.feign;

import com.eduagent.learning.dto.ai.AiChatRequest;
import com.eduagent.learning.dto.ai.AiPathRequest;
import com.eduagent.learning.dto.ai.AiResourceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = AiServiceClientHttpContractTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.jackson.default-property-inclusion=non_null"
        })
class AiServiceClientHttpContractTest {

    private static final AtomicReference<CapturedRequest> LAST_REQUEST = new AtomicReference<>();
    private static final AtomicReference<String> NEXT_RESPONSE = new AtomicReference<>(success("{}"));
    private static final HttpServer SERVER = startServer();

    @Autowired
    private AiServiceClient client;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void aiBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("ai.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }

    @BeforeEach
    void resetServer() {
        LAST_REQUEST.set(null);
        NEXT_RESPONSE.set(success("{}"));
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void chatUsesPrefixedEndpointAndSharedMessageContextBody() throws Exception {
        NEXT_RESPONSE.set(success("{\"answer\":\"多态是……\",\"intent\":\"explain\",\"references\":null}"));

        String response = client.chat(AiChatRequest.of(
                "什么是多态", 1001L, "sess_1", Map.of("course", "JavaSE")));

        CapturedRequest request = LAST_REQUEST.get();
        JsonNode body = objectMapper.readTree(request.body());
        assertEquals("POST", request.method());
        assertEquals("/api/edu-agent-ai/chat", request.path());
        assertEquals("什么是多态", body.path("message").asText());
        assertEquals("1001", body.path("context").path("studentId").asText());
        assertEquals("sess_1", body.path("context").path("sessionId").asText());
        assertEquals("JavaSE", body.path("context").path("profile").path("course").asText());
        assertTrue(response.contains("\"answer\":\"多态是……\""));
    }

    @Test
    void pathUsesPrefixedEndpointAndCarriesCompatibilityStudentId() throws Exception {
        client.generatePath(AiPathRequest.builder()
                .studentId("1001")
                .prompt("生成四周学习路径")
                .profile(Map.of("course", "JavaSE"))
                .build());

        CapturedRequest request = LAST_REQUEST.get();
        JsonNode body = objectMapper.readTree(request.body());
        assertEquals("/api/edu-agent-ai/path/generate", request.path());
        assertEquals("1001", body.path("studentId").asText());
        assertEquals("1001", body.path("student_id").asText());
        assertEquals("JavaSE", body.path("profile").path("course").asText());
    }

    @Test
    void resourceUsesPrefixedEndpointAndSharedModeExtraBody() throws Exception {
        client.generateResource(AiResourceRequest.builder()
                .mode("judge")
                .topic("测验判分")
                .type("judge")
                .extra(Map.of("studentId", "1001", "prompt", "请判分"))
                .build());

        CapturedRequest request = LAST_REQUEST.get();
        JsonNode body = objectMapper.readTree(request.body());
        assertEquals("/api/edu-agent-ai/resource/generate", request.path());
        assertEquals("judge", body.path("mode").asText());
        assertEquals("judge", body.path("type").asText());
        assertEquals("1001", body.path("extra").path("studentId").asText());
        assertEquals("请判分", body.path("extra").path("prompt").asText());
        assertFalse(body.has("resourceType"));
    }

    @Test
    void businessFailureEnvelopeRemainsAvailableToParser() {
        String failure = "{\"code\":500,\"message\":\"LLM 调用失败\",\"data\":null}";
        NEXT_RESPONSE.set(failure);

        String response = client.generateResource(AiResourceRequest.builder()
                .mode("suggestion")
                .type("suggestion")
                .extra(Map.of("prompt", "生成建议"))
                .build());

        assertEquals(failure, response);
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", AiServiceClientHttpContractTest::handle);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("无法启动 AI 契约测试服务", e);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        LAST_REQUEST.set(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                new String(requestBody, StandardCharsets.UTF_8)));

        byte[] response = NEXT_RESPONSE.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String success(String data) {
        return "{\"code\":0,\"message\":\"success\",\"data\":" + data + "}";
    }

    private record CapturedRequest(String method, String path, String body) {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableFeignClients(clients = AiServiceClient.class)
    static class TestApplication {
    }
}
