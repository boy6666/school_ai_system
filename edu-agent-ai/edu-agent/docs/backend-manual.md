# EduAgent 后端开发工作手册

## 1. 手册目的

本文档规范 EduAgent 项目后端开发流程，统一开发环境、项目结构、Git 协作方式、接口开发流程、智能体衔接方式和部署规则。

项目采用前后端分离 + AI 引擎独立部署架构：

```text
前端：Vue 3 + Vite + TypeScript    (edu-agent-web/)
后端：Spring Boot 3 + MyBatis-Plus (edu-agent-server/)
AI  ：Python FastAPI + LangGraph   (edu-agent-ai/)
数据库：MySQL 8.0
接口管理：Apifox
版本管理：Git
部署：Nginx + Spring Boot + Python
```

---

## 2. 开发环境规范

### 2.1 推荐软件

| 软件 | 用途 |
|------|------|
| IntelliJ IDEA | 后端开发 IDE |
| JDK 17+ | Java 运行环境 |
| Maven 3.8+ | 依赖管理与构建 |
| MySQL 8.0 | 数据库 |
| Git for Windows | 版本管理 |
| Apifox | 接口文档、Mock、联调 |
| Postman / curl | 接口测试 |

### 2.2 数据库初始化

```bash
mysql -u root -p < database/init.sql
mysql -u root -p < database/table.sql
mysql -u root -p < database/mock-data.sql
```

### 2.3 启动后端

```bash
cd edu-agent-server
mvn spring-boot:run
```

默认端口：`8080`

### 2.4 启动 AI 引擎

```bash
cd edu-agent-ai
uvicorn api:app --port 8000
```

---

## 3. 项目目录结构

```text
edu-agent-server/
├── src/main/java/com/eduagent/
│   ├── controller/          # 接口层，接收请求/参数校验/返回JSON
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── ProfileController.java
│   │   ├── ResourceController.java
│   │   ├── TutorController.java
│   │   ├── QuizController.java
│   │   ├── TaskController.java
│   │   ├── ReportController.java
│   │   └── AdminController.java
│   │
│   ├── service/             # 业务层，核心逻辑/调用AI/事务管理
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── ProfileService.java
│   │   ├── ResourceService.java
│   │   ├── TutorService.java
│   │   ├── QuizService.java
│   │   ├── TaskService.java
│   │   ├── ReportService.java
│   │   ├── AdminService.java
│   │   └── impl/             # 实现类
│   │
│   ├── mapper/              # 数据访问层（MyBatis-Plus BaseMapper）
│   │   ├── UserMapper.java
│   │   ├── ProfileMapper.java
│   │   ├── ResourceMapper.java
│   │   ├── ConversationMapper.java
│   │   ├── QuestionMapper.java
│   │   ├── TaskMapper.java
│   │   └── ReportMapper.java
│   │
│   ├── entity/              # 数据库实体
│   │   ├── User.java
│   │   ├── StudentProfile.java
│   │   ├── Resource.java
│   │   ├── Conversation.java
│   │   ├── Question.java
│   │   ├── Task.java
│   │   └── Report.java
│   │
│   ├── dto/                 # 请求体对象
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── TutorRequest.java
│   │   └── ...
│   │
│   ├── vo/                  # 响应体对象
│   │   ├── LoginVO.java
│   │   ├── UserInfoVO.java
│   │   └── ...
│   │
│   ├── config/              # 配置类
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   ├── MyBatisPlusConfig.java
│   │   └── FileUploadConfig.java
│   │
│   ├── security/            # 安全鉴权
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── UserDetailsServiceImpl.java
│   │   └── PasswordEncoder.java
│   │
│   ├── agent/               # ★ 智能体衔接层
│   │   ├── AiClient.java          # HTTP调用封装
│   │   ├── AiChatRequest.java     # AI请求体
│   │   ├── AiChatResponse.java    # AI响应体
│   │   └── AiConfig.java          # AI地址配置
│   │
│   ├── common/              # 通用组件
│   │   ├── Result.java            # 统一响应 {code,message,data}
│   │   ├── PageResult.java        # 分页响应
│   │   ├── GlobalExceptionHandler.java
│   │   └── BusinessException.java
│   │
│   └── EduAgentApplication.java   # 启动类
│
├── src/main/resources/
│   ├── application.yml            # 主配置
│   ├── application-dev.yml        # 开发环境
│   ├── application-prod.yml       # 生产环境
│   └── mapper/                    # MyBatis XML（如需要）
│
├── pom.xml
└── README.md
```

---

## 4. 分层规范

### 4.1 Controller 层

只做三件事：**接参数、调 Service、返回结果**。不写任何业务逻辑。使用 `@Valid` 做参数校验，统一用 `Result<T>` 包装返回。

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = authService.login(request);
        return Result.success(vo);
    }
}
```

### 4.2 Service 层

写全部业务逻辑。调用 Mapper 读写数据库。调用 `AiClient` 与 Python AI 交互。需要事务的方法加 `@Transactional`。

```java
@Service
public class TutorServiceImpl implements TutorService {
    @Autowired private AiClient aiClient;
    @Autowired private ConversationMapper conversationMapper;

    @Override
    @Transactional
    public TutorReplyVO chat(TutorRequest request) {
        AiChatResponse aiResp = aiClient.chat(request.getStudentId(), request.getMessage());
        Conversation conv = new Conversation();
        conv.setStudentId(request.getStudentId());
        conv.setQuestion(request.getMessage());
        conv.setAnswer(aiResp.getFinalAnswer());
        conversationMapper.insert(conv);
        return buildReplyVO(aiResp);
    }
}
```

### 4.3 Mapper 层

使用 MyBatis-Plus，继承 `BaseMapper<T>`。复杂查询在 XML 或注解中写 SQL。不写业务逻辑。

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(String username);
}
```

### 4.4 智能体衔接层（agent/）

这是后端与 Python AI 引擎的桥梁。Python 的 `api.py` 提供了 FastAPI 接口：

| Python 接口 | 方法 | 说明 |
|------------|------|------|
| `/chat` | POST | 统一入口，传入 message + studentId |
| `/health` | GET | 健康检查 |

`AiClient.java` 封装 HTTP 调用：

```java
@Component
public class AiClient {
    @Value("${ai.base-url}")
    private String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiChatResponse chat(String studentId, String message) {
        String url = baseUrl + "/chat";
        AiChatRequest req = new AiChatRequest(studentId, message);
        ResponseEntity<AiChatResponse> resp = restTemplate.postForEntity(url, req, AiChatResponse.class);
        return resp.getBody();
    }
}
```

配置文件 `application-dev.yml`：
```yaml
ai:
  base-url: http://localhost:8000
  timeout: 120000
```

**关键注意**：AI 调用耗时较长（资源生成可达 60s），Controller 层不要设置过短超时，必要时用 `@Async` 异步处理。

---

## 5. 安全鉴权

### 5.1 技术选型

- **Spring Security** + **JWT**（无状态 Token）
- 密码加密：BCrypt
- Token 有效期：accessToken 2 小时，refreshToken 7 天

### 5.2 JWT 流程

```
登录 → 验证用户名密码 → 生成 JWT → 返回 Token
请求 → 前端带 Authorization: Bearer {token} → JwtFilter 解析 → 放行
Token 过期 → 前端用 refreshToken 换新 Token
```

### 5.3 角色权限

| 角色 | 权限 |
|------|------|
| `student` | 学习画像、资源、辅导、题库、任务、报告（仅自己的数据） |
| `admin` | 用户管理、数据统计、系统配置、查看所有学生数据 |

方法级鉴权使用 `@PreAuthorize`：

```java
@PreAuthorize("hasRole('admin')")
@GetMapping("/admin/users")
public Result<List<UserVO>> listUsers() { ... }
```

---

## 6. 前端接口对齐

所有接口必须与 `frontend-api-documentation.md` 严格一致。

### 6.1 统一响应格式

```json
{ "code": 200, "message": "成功", "data": {} }
```

后端使用 `Result<T>` 类统一封装。code 含义：

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录/Token过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 6.2 分页格式

请求：`{ "page": 1, "pageSize": 10 }`

响应：`{ "code": 200, "data": { "list": [...], "total": 100, "page": 1, "pageSize": 10 } }`

### 6.3 接口清单

> 完整接口定义见 `frontend-api-documentation.md`，后端开发时以此为唯一权威来源。

| 模块 | 路径前缀 | 主要方法 |
|------|---------|---------|
| 认证 | `/auth/*` | login, register, logout, refresh |
| 用户 | `/user/*` | info, update, avatar |
| 学习画像 | `/profile/*` | get, update, history |
| 资源 | `/resources/*` | list, detail, download |
| 智能辅导 | `/tutor/*` | chat, history, sessions |
| 题库练习 | `/quiz/*` | questions, submit, wrongBook |
| 学习任务 | `/tasks/*` | CRUD |
| 学习报告 | `/reports/*` | generate, list, export |
| 管理后台 | `/admin/*` | users, stats, config |
| 智能体AI | `/ai/*` | chat, resources/generate, health |
| 知识库 | `/kb/*` | list, search |

---

## 7. Git 协作规范

### 7.1 分支策略

```text
main                       # 稳定版本，只通过 PR 合并
├── feature/auth           # A：认证鉴权 + 用户 + 管理后台
├── feature/profile        # B：学习画像 + 资源 + 知识库
├── feature/tutor          # C：智能辅导 + 题库 + 任务 + 报告 + AI衔接
```

### 7.2 工作流程

```bash
git checkout main && git pull origin main
git checkout -b feature/xxx
git add . && git commit -m "feat: 完成登录接口"
git push origin feature/xxx
# 在平台上创建 Pull Request，另两人 Code Review
```

### 7.3 Commit 规范

| 前缀 | 用途 |
|------|------|
| `feat:` | 新功能 |
| `fix:` | 修复bug |
| `docs:` | 文档变更 |
| `refactor:` | 重构 |
| `test:` | 测试相关 |
| `chore:` | 构建/配置 |

### 7.4 冲突处理

- 每天开始前先 `git pull origin main` 同步
- 冲突只在自己分支上解决
- 解决后重新 push，通知 Reviewer

---

## 8. 团队分工

**原则：不按层分，按业务模块垂直切。**

| 成员 | 负责模块 | 包含内容 |
|------|---------|---------|
| **A（骨架）** | 认证鉴权 + 管理后台+智能辅导 | `security/`、`AuthController`、`UserController`、`AdminController`、JWT过滤器、统一异常处理、`Result`封装 |
| **B（业务）** | 学习画像 + 资源 + 知识库+题库 | `ProfileController`、`ResourceController`、文件上传下载、调用AI生成资源、知识库CRUD |
| **C（AI密集）** | 学习任务 + 学习报告 + AI衔接层 + 用户 | `agent/`层、`TutorController`、`QuizController`、`TaskController`、`ReportController`、AI调用封装 |

### 开发顺序

```
Week 1
  A: 项目初始化、Security+JWT、登录/注册
  B: 数据库实体设计、画像接口
  C: agent/AiClient封装、AI对话接口

Week 2
  A: 用户管理、管理后台基础
  B: 资源CRUD、文件存储
  C: 题库接口、辅导历史、学习任务CRUD

Week 3
  A: 权限细化、管理后台统计
  B: 知识库接口、资源下载
  C: 学习报告生成、全链路联调

Week 4
  全员联调 + Apifox同步 + bug修复 + 部署测试
```

### 联调依赖

```
A 先完成 auth  →  B/C 的接口才能加 @PreAuthorize
A 先完成 Result →  B/C 的返回值统一格式
C 先完成 AiClient → B 的资源生成接口才能调用 AI
```

---

## 9. 智能体衔接规范

### 9.1 架构关系

```text
前端 (edu-agent-web)
  │  POST /tutor/chat
  ▼
Spring Boot (edu-agent-server)
  │  AiClient.chat(studentId, message)
  ▼
Python AI (edu-agent-ai)
  │  LangGraph pipeline
  ├── safety_agent    ├── router_agent
  ├── explain_agent   ├── resource_agent
  ├── tutor_agent     ├── quiz_agent
  ├── path_agent      ├── retrieval_agent
  ├── evaluation_agent├── profile_agent
  │  return { intent, final_answer, evaluation_report, ... }
  ▼
Spring Boot
  │  存对话记录、更新画像、返回前端
  ▼
前端
```

### 9.2 AiClient 调用规范

```java
AiChatResponse resp = aiClient.chat(studentId, message);
String answer = resp.getFinalAnswer();         // 用户可见回答
String intent = resp.getIntent();              // 意图分类
EvaluationReport eval = resp.getEvaluationReport(); // 评估报告
SafetyReport safety = resp.getSafetyReport();       // 安全审查
```

### 9.3 AI 返回字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `intent` | String | explain / quiz / retrieve / resource_generation / tutoring |
| `intent_confidence` | Float | 置信度 0~1 |
| `final_answer` | String | Markdown 格式回答 |
| `profile` | Object | 学生画像 |
| `profile_patch` | Object | 本轮画像变更 |
| `evaluation_report` | Object | {understanding_score, weak_points, suggestion} |
| `safety_report` | Object | {passed, risk_level, issues} |
| `resource_dir` | String | 生成资源落盘路径 |

### 9.4 注意事项

- **超时**：AI 调用默认超时 120s，资源生成场景可能需要更长
- **模型配置**：Python 端 `.env` 的 `AGENT_*_MODEL` 为每个智能体指定模型，后端不需要关心
- **Mock 模式**：Python 端 `MOCK_LLM=1` 时不调用真实 API，后端联调可先用 Mock 模式跑通流程
- **调试模式**：Python 端 `AGENT_DEBUG=1` 时打印每个智能体的调用日志

---

## 10. 代码规范

### 10.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `AuthController`, `JwtTokenProvider` |
| 方法名 | 小驼峰 | `getUserInfo`, `sendMessage` |
| 变量名 | 小驼峰 | `studentId`, `loginRequest` |
| 常量 | 全大写+下划线 | `MAX_LOGIN_ATTEMPTS` |
| 包名 | 全小写 | `com.eduagent.controller` |
| URL | 小写+短横线 | `/user/info`, `/tutor/chat` |

### 10.2 异常处理

- 业务异常统一抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 捕获转成 `Result`
- 不要吞异常，不要 `catch(Exception e) {}` 空处理
- Controller 层不写 try-catch，交给全局处理器

### 10.3 日志规范

```java
@Slf4j
public class AuthService {
    public LoginVO login(LoginRequest req) {
        log.info("用户登录: username={}", req.getUsername());
        // ...
    }
}
```

---

## 11. 测试规范

- Service 层写单元测试（JUnit 5 + Mockito）
- Controller 层写集成测试（Spring Boot Test + MockMvc）
- 至少覆盖核心流程：登录、对话、资源生成
- AI 调用层使用 Mock 避免依赖 Python 服务

---

## 12. 部署架构

```text
                    Nginx :80
                   /          \
        Vue 静态文件          /api/* → Spring Boot :8080
                                     │
                         ┌───────────┴───────────┐
                    Python AI :8000        MySQL :3306
```

Nginx 配置（`nginx/edu-agent.conf`）：

```nginx
server {
    listen 80;
    location / {
        root /path/to/edu-agent-web/dist;
        try_files $uri /index.html;
    }
    location /api/ {
        proxy_pass http://localhost:8080/;
    }
    location /uploads/ {
        alias /path/to/uploads/;
    }
}
```

---

## 13. 常见问题

| 问题 | 排查方向 |
|------|---------|
| 前端返回 401 | 检查 Authorization 头、Token 是否过期 |
| AI 调用超时 | 检查 Python 是否启动（`:8000`）、`.env` API Key |
| 数据库连接失败 | 检查 `application-dev.yml` MySQL 配置 |
| 跨域错误 | 检查 `CorsConfig` 前端地址（默认 `localhost:5173`） |
| 文件上传失败 | 检查 `file.upload-dir` 路径权限 |
