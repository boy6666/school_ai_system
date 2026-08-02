package com.eduagent.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 跨服务调用“鉴权服务”的 Feign 契约（示范）。
 *
 * <p>调用方（任意注入了本客户端的微服务）拿到的是【调用方自身】的身份——因为 {@code AuthFeignInterceptor}
 * 会把本服务 {@link com.eduagent.common.security.AuthContext} 中的 X-User-Id / X-User-Roles 透传到
 * 下一跳；鉴权服务的 {@code /me} 依据该头返回对应用户。整条信任链为：
 * 网关验签 → 注入 X-User-* → 本服务 AuthContext → Feign 透传 → 下游 AuthContext → /me。
 *
 * <p>注意：Feign 直连目标服务（lb://edu-agent-auth），不走网关，因此下游信任内部注入的头、
 * 不再二次验签。仅网关对外暴露，客户端无法伪造 X-User-*。
 *
 * <p>启用方式：在消费方启动类加 {@code @EnableFeignClients(basePackages = "com.eduagent")}，
 * 然后直接 {@code @Autowired UserClient}。无需自行写 OkHttp/RestTemplate。
 */
@FeignClient("edu-agent-auth")
public interface UserClient {

    @GetMapping("/api/edu-agent-auth/me")
    UserInfo me();
}
