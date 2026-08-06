# 框架开发 / 复用指南（FRAMEWORK_DEV_GUIDE）

> 受众：在 EduAgent 微服务骨架上做业务开发的同事（陈海洋、陈嘉成、曾姿妍，以及后续接手者）。
> 配套文档：
> - `docs/dev-prod-guide.md` —— 本机单环境（local）怎么跑、如何联调、Git 协作。本文**不重复**部署与 Git，只讲「代码层面怎么基于框架开发 / 复用公共能力」。
> - 各人子 spec（`docs/superpowers/specs/dev-*.md`）—— 业务接口契约与数据模型，按模块分工落地。
>
> 本文以 **common / common-mybatis / 网关安全 / Feign / Flyway / MyBatis-Plus** 为核心，给出可照抄的范式。

---

## 0. 一分钟认知

- 单仓多 Maven module：父 `edu-agent`（0.1.0），子模块 `edu-agent-gateway` / `edu-agent-auth` / `edu-agent-learning` / `edu-agent-resource` / `edu-agent-teacher` / `edu-agent-code`，外加两个**非业务**模块 `edu-agent-common`（公共能力）、`edu-agent-common-mybatis`（MyBatis-Plus 基座）。ai-service 是独立 Python 进程（`edu-agent-ai/`），不在此仓。
- **DB-per-service**：每个 Java 服务一个库（auth_db / learning_db / resource_db / teacher_db / code_db），跨服务一律走 **Feign / MQ**，**禁止直连对方库**。
- **网关是唯一鉴权点 + 唯一浏览器 CORS 边界**：JWT 在网关验，验过后注入 `X-User-Id` / `X-User-Roles` 头转发下游；下游服务**只信**这两个头，自己不解析 JWT。
- **common 不依赖 Web 层**：`Result` / `AuthContext` / `JwtUtil` 等可安全放在 common；但 `@RestControllerAdvice` 全局异常处理器、servlet 过滤器**必须留在各服务内**（见 §4）。
- 技术栈：Spring Cloud 2023.0.1 + Spring Boot 3.2.5，Java 17 编译（release 17），本机可跑 JDK 21。父 pom 已统一版本。

---

## 1. 如何新建 / 接入一个微服务

> 适用于：新增一个 Java 业务服务（如未来的 `edu-agent-x`）。已有 5 个服务直接跳到 §3。

### 1.1 步骤清单

1. **父 pom 加 module**：在 `edu-agent/pom.xml` 的 `<modules>` 加 `<module>edu-agent-x</module>`，并确认 `dependencyManagement` 里已锁定 `edu-agent-common` / `edu-agent-common-mybatis` 版本（父 pom 已管）。
2. **建 module 目录与 pom**：复制 `edu-agent-code` 的 `pom.xml` 骨架，改 `artifactId`、`description`、`<name>`。依赖至少包含：
   ```xml
   <dependency><groupId>com.eduagent</groupId><artifactId>edu-agent-common</artifactId></dependency>
   <dependency><groupId>com.eduagent</groupId><artifactId>edu-agent-common-mybatis</artifactId></dependency>
   <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
   <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
   <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId></dependency>
   <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId></dependency>
   <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
   <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-loadbalancer</artifactId></dependency>
   <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
   <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
   <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-boot-starter</artifactId></dependency>
   <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
   <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
   ```
   > **网关例外**：`edu-agent-gateway` 是 WebFlux，**只依赖 `edu-agent-common`，不依赖 `edu-agent-common-mybatis`**（避免 MyBatis 把 SqlSessionFactory 拉进 WebFlux 导致无数据源启动失败）。
3. **启动类**：包名 `com.eduagent.x`，加注解：
   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   @EnableFeignClients(basePackages = "com.eduagent")   // 扫到 common.feign.UserClient 等
   @MapperScan("com.eduagent.x.mapper")
   public class XServiceApplication { public static void main(String[] a){ SpringApplication.run(XServiceApplication.class, a); } }
   ```
4. **Nacos 配置**：在 `deploy/nacos-config/edu-agent-x.yaml` 放该服务的配置（datasource / mybatis-plus / rabbitmq / management 等），由 `push-nacos-config.py` 推到唯一的 `edu-agent-local` namespace。**服务名必须 = `edu-agent-x`**（与 `ServiceConstants.SVC_*` 一致，网关按 `/api/x/**` 路由，Feign 按同名发现）。
5. **库与 Flyway**：新建空库 `x_db`；在 `src/main/resources/db/migration/` 放 `V1__init.sql`（建表，不建库，见 §5）。
6. **逻辑删除全局配置**（只要用 `BaseEntity`，必须配）：在服务的 Nacos yaml 里加
   ```yaml
   mybatis-plus:
     global-config:
       db-config:
         logic-delete-field: deleted      # 全局逻辑删除字段名
         logic-not-delete-value: 0
         logic-delete-value: 1
   ```
7. **健康检查**（§6 已统一示例）：在 Nacos yaml 暴露 `management.endpoints.web.exposure.include: health,info`。

---

## 2. 服务命名 / 路由 / 网关前缀（硬约束）

| 维度 | 规则 |
|------|------|
| Nacos 服务名 | `edu-agent-<svc>`（如 `edu-agent-auth`）。`ServiceConstants.SVC_*` 已定义，引用它别硬编码。 |
| 网关路由前缀 | `/api/<svc>/**`（如 `/api/learning/**`）。网关**不 StripPrefix**，下游 Controller 的 `@RequestMapping` 要带完整 `/api/<svc>`。 |
| Feign 目标名 | 与 Nacos 服务名一致（`@FeignClient("edu-agent-auth")`）。 |
| 跨服务路径 | Java 侧调 ai-service 的路径**必须带 `/api/ai` 前缀**（与网关转发一致，网关不 StripPrefix）；否则直连 404。 |

> 看 `edu-agent-code/.../controller/CodeController.java` 的 `@RequestMapping("/api/code")` 与 `edu-agent-auth/.../controller/AuthController.java` 的 `/api/edu-agent-auth` 作为对照。

---

## 3. common 公共能力清单与用法

包根 `com.eduagent.common`。`CommonAutoConfiguration` 用 `@ComponentScan("com.eduagent.common")` 自动把 common 里的 `@Component` 扫进所有依赖它的服务（servlet 服务自动获得 `AuthContextFilter` / `AuthFeignInterceptor` / `JwtUtil` 等，无需各服务再 `@ComponentScan`）。

### 3.1 统一响应 `Result<T>` / `PageResult<T>`

```java
return Result.success(data);                       // code=0, message="success"
return Result.success();                           // 无 data
return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "参数错误");
return Result.fail("系统忙");                       // code=500
// 分页
PageResult.of(records, page.getTotal());           // records=本页数据, total=总条数
```
- 所有接口返回**必须**包 `Result<T>`（或 `Result<PageResult<T>>`）。wire 字段名 camelCase 由 Spring 的 jackson 默认保证（契约 C4）。
- `PageResult` 仅承载 `total` + `list`，配合 MyBatis-Plus 的 `Page<T>` 使用：`Page<User> p = userMapper.selectPage(new Page<>(1,20), q); PageResult.of(p.getRecords(), p.getTotal());`

### 3.2 错误码 `ErrorCode` / 业务异常 `ApiException`

```java
throw new ApiException(ErrorCode.NOT_FOUND);                       // 用预定义码
throw new ApiException(ErrorCode.CONFLICT, "该作业已发布");          // 带明细
throw new ApiException(4001, "自定义业务码");                        // 自定义 code
```
`ErrorCode` 枚举含 `SUCCESS/BAD_REQUEST(400)/UNAUTHORIZED(401)/FORBIDDEN(403)/NOT_FOUND(404)/CONFLICT(409)/SYSTEM_ERROR(500)`。业务可在 `code` 上扩展自己的段（如 4xxx）。

### 3.3 全局异常处理（★各服务自备，common 不提供）

`ApiException` 在 common，但**把它转成 `Result` 的 `@RestControllerAdvice` 必须写在各 servlet 服务里**（common 刻意不依赖 Web 层，见 §0）。每个服务放一个：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public Result<Void> handleApi(ApiException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ":" + f.getDefaultMessage()).findFirst().orElse("参数错误");
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("unexpected", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常");
    }
}
```
> 用 `spring-boot-starter-validation` 的参数校验（`@NotNull`/`@Size` 等）配合上面第二段即可。缺这个 Advice 时 `ApiException` 会直接 500 而非被包成 `Result`。

### 3.4 身份 `AuthContext` / `AuthContextFilter`（下游读身份）

下游服务**不解析 JWT**，只读网关注入的头：

```java
Long studentId = Long.parseLong(AuthContext.getUserId());   // 字符串，按需转 Long
List<String> roles = AuthContext.getRoles() == null ? List.of()
                        : List.of(AuthContext.getRoles().split(","));
```
- `AuthContextFilter`（common 里 `@ConditionalOnWebApplication(SERVLET)`）在请求进入时把 `X-User-Id` / `X-User-Roles` 写入 `AuthContext`，`finally` 里 `clear()`（避免线程复用串号）。**网关是 WebFlux，不注册此 filter**，所以网关自己解析 JWT、下游只信头，职责清晰。
- 角色常量用 `ServiceConstants.ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN`，别写字符串字面量。

### 3.5 JWT 工具 `JwtUtil`（仅 auth-service 用）

签发在 auth-service（`generateToken(userId, roles)`），解析在**网关** `AuthGlobalFilter`。业务服务**不应**再调用 `JwtUtil`。密钥 `edu-agent.jwt.secret`（≥32 字节，HS256），全网（含网关）必须共享同一份。

### 3.6 Feign 透传拦截器 `AuthFeignInterceptor`（自动生效）

服务启用 `@EnableFeignClients` 后，所有出站 Feign 请求自动带上当前 `AuthContext` 的 `X-User-Id` / `X-User-Roles` 头，**下游据此拿到调用方身份**。**不要**在 Feign 请求里手填 `X-User-*` 头（会被网关视为伪造，且破坏透传一致性）。

### 3.7 跨服务常量 `ServiceConstants`

所有「服务名 / Nacos group / 角色 / 透传头 / MQ 事件名」集中在此，引用而非硬编码，防止漂移（契约 C11/C12）：

```java
ServiceConstants.SVC_AUTH;                 // "edu-agent-auth"
ServiceConstants.GROUP_RESOURCE;          // "resource-group"
ServiceConstants.ROLE_TEACHER;            // "ROLE_TEACHER"
ServiceConstants.HEADER_USER_ID;          // "X-User-Id"
ServiceConstants.EVENT_ASSIGNMENT_GRADED; // "assignment.graded"
```

### 3.8 MQ 事件基类 `BaseEvent`

```java
public class AssignmentGradedEvent extends BaseEvent {   // 自动带 eventId / timestamp
    private Long assignmentId;
    // ... 业务字段
}
```
约定：**exchange 名 = 事件名**（如 `assignment.graded`），见 `ServiceConstants.EVENT_*`。消息队列配置（队列/交换机绑定）在各服务内用 `spring-boot-starter-amqp` + Nacos yaml 声明，common **不提供** `RabbitConfig`（保持 common 非 Web/非 DB 的纯净性）。

### 3.9 跨服务查用户示范 `UserClient` / `UserInfo`

common 已提供调 auth-service 的示范 Feign 客户端，业务服务可直接注入使用：

```java
@FeignClient("edu-agent-auth")                 // 目标 = ServiceConstants.SVC_AUTH
public interface UserClient {
    @GetMapping("/api/edu-agent-auth/me")
    UserInfo me();
}
```
`UserInfo`（`com.eduagent.common.feign`）字段：`userId, username, realName, roles(List<String>), status, email, phone`。`edu-agent-code` 的 `CodeController.whoami()` 是现成用法示例（注入 `UserClient` → 调 `me()` → 包 `Result<UserInfo>` 返回）。要复用，消费方启动类 `@EnableFeignClients(basePackages = "com.eduagent")` 即可。

---

## 4. common-mybatis 基座用法

`edu-agent-common-mybatis` 是独立 starter，自动装配 MyBatis-Plus 基础设施，**DB 服务依赖它、网关不依赖**（见 §1.2）。

### 4.1 实体继承 `BaseEntity`（审计字段 + 主键 + 逻辑删除）

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_exercises")
public class CodeExercise extends BaseEntity {     // 只写业务字段
    private String title;
    private String difficulty;   // EASY/MEDIUM/HARD
    private String language;
    private Integer status;      // 1启用 0禁用
}
```
`BaseEntity` 已含：`id`（`@TableId` 自增）、`createTime` / `updateTime`（由 `AutoFillMetaObjectHandler` 在插入/更新时**自动填充**，实体无需手动赋值）、`deleted`（逻辑删除）。

> **★逻辑删除硬约束**：`deleted` 依赖各服务 Nacos 里的 `mybatis-plus.global-config.db-config.logic-delete-field=deleted` 全局生效（见 §1.6）。因此**继承 `BaseEntity` 的实体，其数据表必须有 `deleted` 列**——用 Flyway 加列（如 `edu-agent-code` 的 `V2__add_deleted.sql`：`ALTER TABLE code_exercises ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0`）。**没有 `deleted` 列的表不要继承 `BaseEntity`**（如 auth 的 users / roles / role_user），否则逻辑删除会出错。

### 4.2 自动填充 `AutoFillMetaObjectHandler`

业务无感：插入自动填 `createTime`+`updateTime`，更新自动填 `updateTime`。无需任何额外配置（starter 已 `@Component` + 自动装配）。

### 4.3 分页拦截器 `MybatisPlusConfig`

已注册 `PaginationInnerInterceptor`，直接 `new Page<>(page, size)` + `selectPage(...)` 即可分页，无需各服务重复声明。

### 4.4 Mapper

```java
@Mapper
public interface CodeExerciseMapper extends BaseMapper<CodeExercise> { }
```
（配合启动类 `@MapperScan("com.eduagent.x.mapper")`）。复杂 SQL 放 `src/main/resources/mapper/*.xml`，`mybatis-plus.mapper-locations: classpath*:mapper/*.xml`。

---

## 5. 数据库与 Flyway 约定

- **Flyway 只建表、不建库**。每个服务启动连自己的库，库由 `database/init-microservice.sql`（或 deploy 的初始化）先建好空库 + 授权用户。
- 迁移脚本放 `src/main/resources/db/migration/`，命名 `V{n}__{描述}.sql`（如 `V1__init.sql`、`V2__add_deleted.sql`）。**已执行的脚本禁止修改**；要改表就新增 `V{n+1}__*.sql`。
- 同一套迁移在唯一的 `edu-agent-local` 命名空间跑，且只跑一次（记在 `flyway_schema_history`）。
- 种子数据写 `V2__seed.sql` 之类（配合前端零 mock 策略），注意幂等（用 `INSERT IGNORE` / `ON DUPLICATE KEY UPDATE`）。
- 跨库外键一律删除，改为**逻辑引用**（纯 BIGINT 字段）。例如 `student_profiles.student_id` 逻辑关联 `auth_db.users.id`，不建 FK。

---

## 6. 网关安全模型（调用方必读）

```
浏览器 ──Authorization: Bearer <JWT>──▶ Gateway
        Gateway.AuthGlobalFilter:
          1) 白名单(/health, 登录, OPTIONS 预检) 直接放行
          2) 验 JWT ──失败──▶ 401
          3) headers.remove("X-User-*")      // 先剥离客户端伪造头
          4) headers.set X-User-Id / X-User-Roles  // 注入可信身份
          5) 转发下游 /api/<svc>/**
        ▼
   下游服务: AuthContextFilter 读头 → AuthContext.set(...) → 业务用 AuthContext.getUserId()
```
要点：
- **OPTIONS 预检**在网关被放行（不进 JWT 校验），CORS 由网关 `globalcors` 统一处理。
- 下游**永远只信网关注入的头**，绝不在业务里解析 JWT 或信任客户端带来的 `X-User-*`。
- 内网专用端点（如 `ai-service` 的 `/api/ai/code/analyze`）**不进网关路由表**，仅 Java 服务经 Feign 直连。

---

## 7. 跨服务调用（OpenFeign）约定

```java
@FeignClient(name = "edu-agent-learning", path = "/api/learning")   // 路径带前缀，与网关一致
public interface LearningServiceClient {
    @GetMapping("/analytics/student/{studentId}")
    Result<StudentAnalyticsVO> getAnalytics(@PathVariable("studentId") Long studentId);
}
```
- 路径**带 `/api/<svc>` 前缀**，与网关转发一致（网关不 StripPrefix）。
- 身份由 `AuthFeignInterceptor` 自动透传，调用方**不要**手填 `X-User-*`。
- 容错：对易抖动下游（如 ai-service、code-service 判分）务必设短超时 + fallback，避免上游被拖死（Sentinel 限流在 P4 固化）。
- 想直接复用查用户，注入 common 的 `UserClient`（§3.9）。

---

## 8. 常见坑位清单

| 坑 | 现象 / 解决 |
|---|---|
| 忘了配逻辑删除字段 | 继承 `BaseEntity` 但表无 `deleted` 列 → 启动或查询异常。**表必须有 `deleted` 列**（Flyway 加）。 |
| 在 common 写 `@RestControllerAdvice` | common 不依赖 Web 层，**编译不过**。全局异常处理器放各服务。 |
| 网关依赖 common-mybatis | WebFlux 网关拉进 SqlSessionFactory → 无数据源启动失败。**网关只依赖 common**。 |
| Feign 路径漏 `/api/<svc>` | 直连 404。路径前缀与网关一致。 |
| 手填 `X-User-*` 头 | 被网关视为伪造 / 破坏透传一致性。用 `AuthContext`，让 `AuthFeignInterceptor` 自动带。 |
| 下游自己解析 JWT | 职责错位。下游只读 `AuthContext`。 |
| 跨库直连对方表 | 违反 DB-per-service。走 Feign/MQ。 |
| 改已执行的 Flyway 脚本 | Flyway 校验失败。新增 `V{n+1}`。 |
| 服务名 ≠ `edu-agent-<svc>` | 网关路由 / Feign 发现都对不上。用 `ServiceConstants.SVC_*`。 |
| `@EnableFeignClients` 没扫到 common.feign | `UserClient` 注入失败。启动类用 `basePackages="com.eduagent"`。 |

---

## 9. 与本文档配套的其它文档

- `docs/dev-prod-guide.md` —— 本机单环境开发/联调、Git 协作规范。
- `docs/superpowers/specs/2026-07-31-p0-infra-gateway.md` —— 网关 / 白名单 / JWT 透传的权威设计。
- `docs/superpowers/specs/dev-*.md` —— 各模块（auth/learning/resource/teacher/code/ai）的接口契约与数据模型，按模块分工落地。
- `docs/GIT_CONVENTIONS.md` —— 分支模型、提交格式、CI 门禁。
