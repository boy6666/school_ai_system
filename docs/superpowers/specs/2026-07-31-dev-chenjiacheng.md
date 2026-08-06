# resource-service 开发文档（子 Spec · 成员B 陈嘉成）

> 阶段：P1–P2 ｜ 负责人：**陈嘉成（成员B）** ｜ 状态：待评审（主蓝图 §3.3 / §12.1 已分配）
> 依赖前置：**P0 已完成**（Nacos / Gateway / auth / common / JWT 透传 / Redis / RabbitMQ 就绪）
> 本文把主蓝图 §3.3「resource-service」+ §8「RabbitMQ 异步」+ P0 §3「common」展开为**可直接开发**的子 spec。
> 粒度遵循 P0：**需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收**。
> 契约依赖：**陈海洋的 ai-service（端口 8001）**、**陈海洋的 learning-service（端口 8082）**。陈海洋的 ai-service 子 spec 尚未生成，本文以主蓝图 + 现有 `edu-agent-ai/api.py` 为基准推导契约，并在 §5 列出对齐清单与漂移风险。

---

## 0. 服务速览（一句话定位）

`resource-service` 是**最轻的业务服务**：自己不跑 LLM，只负责「资源 CRUD + 缓存 + 异步编排」，真正的生成全部通过 **OpenFeign 调 ai-service**；学生画像按需 Feign 拉 `learning-service`。库 `resource_db`，端口 `8083`，网关前缀 `/api/resource/**`。

```
前端 ──(Bearer)──▶ Gateway ──X-User-Id/X-User-Roles──▶ resource-service:8083
                                                        │  ├─ Feign ▶ ai-service:8001   (/api/ai/resource/generate …)
                                                        │  ├─ Feign ▶ learning-service:8082 (/api/learning/profile)
                                                        │  ├─ Redis 缓存热门章节资源
                                                        │  └─ RabbitMQ 发 resource.generate 任务 → 同服务 worker 消费
```

---

## 1. resource-service 职责与边界

### 1.1 归我（resource-service）做的事
- 资源(Resource)全生命周期：列表 / 详情 / 按章节查询 / 生成 / 重新生成 / 删除 / 收藏 / 点赞反馈。
- **生成编排**：接收生成请求 → 建「生成中」资源行 → 发 RabbitMQ 任务 → worker 调 ai-service → 回填内容 / 状态 → 写 Redis 缓存。
- **缓存**：热门章节资源（`resource:chapter:{chapterId}:{type}`）走 Redis，命中直接返回，避免重复打 ai。
- **内容状态机**：`generating → published / failed`（蓝图 §3.3 提到的内容审核流，本期先做生成态，审核/下架留 P3 治理）。
- **画像消费**：生成时按需 Feign 拉取当前学生的 `learning-service` 画像，拼进 ai 请求（原单体直接查 `student_profiles`，微服务禁止直连对方库）。

### 1.2 不归我（边界，避免后期扯皮）
| 能力 | 归属服务 | 说明 |
|------|----------|------|
| **真正的 LLM 生成 / RAG 检索** | ai-service（陈海洋） | 我只 Feign 调，不持有 Chroma / LLM。 |
| **学生画像 / 学习路径 / 任务 / 学习日志** | learning-service（陈海洋） | 我仅**消费**画像（只读 Feign），不写。 |
| **代码沙箱 / 静态检查 / 代码作业判分** | code-service（吴友诚·P2） | 我生成的 `code` 类资源只是「教学级案例文本」，不是沙箱运行。沙箱是 code-service 的事。 |
| **教师题库 / 班级 / 作业 / 成绩** | teacher-service（吴友诚·P3） | 我不维护题库与班级。 |
| **管理端审计 / 统计治理页** | teacher-service / admin（P3） | 本期我只暴露 `status=failed` 供治理页查询，不自建治理。 |
| **对话 / 讲解 / 判分** | ai-service + learning-service | `/api/ai/chat` 由前端/网关直连 ai-service，不经我中转（见 §2 注）。 |

### 1.3 角色矩阵
- `ROLE_STUDENT`：自己的资源 CRUD、生成、收藏、点赞；能看到 `status=published` 的资源。
- `ROLE_TEACHER`：可发布/查看章节公共资源、查看本班学生资源（P3 拓展）。
- `ROLE_ADMIN`：全量查看、下架 `failed`/违规资源、统计。
- JWT 透传：我不解析 JWT，统一用 `AuthContext.getUserId()`（P0 §3.2 / §6）。

---

## 2. 接口契约（网关前缀 `/api/resource/**`）

> 统一响应：`com.eduagent.common.Result<T>`（`{code,message,data}`），与 P0 `common` 一致。
> 错误码：200 成功；400 参数错；401 未登录（网关拦）；403 越权；404 资源不存在；409 重复；502 AI 不可用（降级友好语）；504 生成超时。
> 大生成异步：返回 HTTP `202 Accepted` + `location` 指向查询端点，客户端轮询详情即可看到 `status` 变化。

### 2.1 端点总表

| # | 方法 | 路径 | 说明 | 角色 | 同步/异步 | 调 ai-service? |
|---|------|------|------|------|-----------|----------------|
| 1 | GET | `/api/resource` | 资源列表（分页+筛选） | S/T/A | 同步 | 否 |
| 2 | GET | `/api/resource/{id}` | 资源详情（views+1） | S/T/A | 同步 | 否 |
| 3 | GET | `/api/resource/chapter/{chapterId}` | 某章节全部资源 | S/T/A | 同步 | 否 |
| 4 | GET | `/api/resource/chapter/{chapterId}/{type}` | 章节指定类型资源（命中缓存/已生成直接返，否则触发异步生成） | S | 同步返回**当前态**（生成中则返回 `status=generating`，轮询） | 是（若需生成） |
| 5 | POST | `/api/resource/generate` | 主动触发一次生成（mindmap/quiz/reading/code/…） | S | **异步 202**（quiz/reading/code/learning_path 走 MQ） | 是 |
| 6 | POST | `/api/resource/{id}/regenerate` | 按新难度重新生成 | S(owner) | 异步 202 | 是 |
| 7 | POST | `/api/resource/{id}/favorite` | 收藏/取消收藏 | S | 同步 | 否 |
| 8 | GET | `/api/resource/favorites/mine` | 我的收藏列表 | S | 同步 | 否 |
| 9 | POST | `/api/resource/{id}/feedback` | 点赞/难度反馈 | S | 同步 | 否 |
| 10 | DELETE | `/api/resource/{id}` | 删除资源（owner 或 ADMIN） | S(owner)/A | 同步 | 否 |

> 注：蓝图 §6.4 的 `POST /chat` 是 ai-service 直连端点，**前端经网关 `/api/ai/chat` 直调 ai-service**，不经本服务（保持本服务「只管资源」的纯粹性）。本服务仅依赖 ai-service 的 `/api/ai/resource/generate` 与 `/api/ai/path/generate`。

### 2.2 端点逐条契约

#### 2.2.1 GET `/api/resource`
查询参数：
```json
{ "page": 1, "pageSize": 20, "keyword": "可选", "type": "可选 mindmap|quiz|reading|code|learning_path", "status": "可选 generating|published|failed" }
```
响应（data）：
```json
{
  "list": [
    { "id": 101, "title": "Java 基础 - 思维导图", "type": "mindmap", "difficulty": "medium",
      "chapter": "面向对象", "status": "published", "rating": 4.5, "views": 32, "favorites": 5,
      "createTime": "2026-07-31T10:00:00" }
  ],
  "total": 128, "page": 1, "pageSize": 20
}
```

#### 2.2.2 GET `/api/resource/{id}`
响应 data：`ResourceVO`（含全部字段：title/type/difficulty/chapter/content/status/rating/views/favorites/createTime）。
- 副作用：`views + 1`（异步计数，不阻塞返回）。
- 若 `status=failed`：返回 200 但 `content=null` + `errorMsg="AI 生成失败：<原因>"`，前端展示友好重试按钮。

#### 2.2.3 GET `/api/resource/chapter/{chapterId}`
响应 data：`ResourceVO[]`（该章节下全部资源，按 `create_time desc`）。

#### 2.2.4 GET `/api/resource/chapter/{chapterId}/{type}`
查询参数：`difficulty=medium`、`chapterName=`（章节展示名）、`topic=`（知识点）。
- **命中**：已存在 `published` 同章节同类型资源 → 直接返回（同步，202 内的「瞬间」体验）。
- **未命中 / `generating`**：返回已建的行（status=generating）或触发异步生成后返回 `status=generating`（202 语义，但用 200 + status 字段以便前端无刷新轮询）。
- 前端轮询此端点直到 `status=published`。
```json
// 生成中
{ "id": 102, "status": "generating", "type": "quiz", "chapter": "集合" }
// 完成
{ "id": 102, "status": "published", "type": "quiz", "content": "[{\"question\":\"...\"}]" }
```

#### 2.2.5 POST `/api/resource/generate`（异步 202）
请求体：
```json
{
  "chapterId": "12",
  "chapterName": "Java 集合框架",
  "topic": "HashMap 底层原理",
  "type": "quiz",                 // mindmap|quiz|reading|code|learning_path
  "difficulty": "medium",         // easy|medium|hard
  "force": false                 // true 跳过缓存强制重生成
}
```
响应 `202 Accepted`，data：
```json
{ "id": 103, "status": "generating", "location": "/api/resource/103" }
```
- 服务端动作：① 若 `!force` 且已存在 `published` 同章节同类型同难度 → 直接返回该行（不进 MQ）；② 否则插入 `status=generating` 行 → 发 `ResourceGenerateMessage` 到 `resource.generate` → 返回 202。

#### 2.2.6 POST `/api/resource/{id}/regenerate`（异步 202）
请求体：`{ "difficulty": "hard" }`。把该行 `status` 重置为 `generating`，发 MQ（带新难度），返回 202。

#### 2.2.7 POST `/api/resource/{id}/favorite`
请求体：`{ "favorite": true }`（true 收藏 / false 取消）。响应 `Result<Void>`；`favorites` 计数同步更新。

#### 2.2.8 GET `/api/resource/favorites/mine`
响应 data：`ResourceVO[]`（当前用户收藏的资源）。

#### 2.2.9 POST `/api/resource/{id}/feedback`
请求体：`{ "liked": true, "difficultyFeedback": "太难了" }`。记录点赞（`favorites` 仅由 2.2.7 控制，此处 `liked=true` 仅写画像反馈表或累加 rating 权重，按 P3 治理决定；本期写入 `resource_feedback` 表）。响应 `Result<Void>`。

#### 2.2.10 DELETE `/api/resource/{id}`
- 仅 `user_id == 当前用户` 或 `ROLE_ADMIN` 可删，否则 403。
- 删后清 Redis 缓存。响应 `Result<Void>`。

---

## 3. 数据模型（`resource_db`）

### 3.1 DDL（MySQL 8 / InnoDB / utf8mb4）
> DB-per-service：本服务独占 `resource_db`，**禁止任何服务直连本库**。跨服务读一律走 Feign。

```sql
CREATE DATABASE IF NOT EXISTS resource_db
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resource_db;

-- 3.1.1 核心资源表（迁移自单体 edu_agent.resources，对齐 Resource 实体 + 微服务字段重命名）
CREATE TABLE learning_resources (
  id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT       NOT NULL                COMMENT '归属学生(原 student_id)',
  title         VARCHAR(200) NOT NULL                COMMENT '资源标题',
  type          VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT 'mindmap|quiz|reading|code|learning_path|review|summary',
  difficulty    VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT 'easy|medium|hard',
  chapter       VARCHAR(100) DEFAULT NULL            COMMENT '章节展示名(原 chapter_name)',
  chapter_id    VARCHAR(50)  DEFAULT NULL            COMMENT '章节ID(原 course_id)',
  course_name   VARCHAR(100) DEFAULT NULL            COMMENT '课程名',
  description   TEXT                               COMMENT '描述',
  content       LONGTEXT                           COMMENT 'AI 生成内容(JSON/Markdown 文本)',
  prompt        TEXT                               COMMENT '触发本次生成的 prompt(复现/审计用)',
  ai_task_id    VARCHAR(64)  DEFAULT NULL            COMMENT '关联 MQ 任务 id(异步去重/追踪)',
  status        VARCHAR(20)  NOT NULL DEFAULT 'generating' COMMENT 'generating|published|failed|archived',
  error_msg     VARCHAR(500) DEFAULT NULL            COMMENT '生成失败原因(降级展示)',
  rating        DECIMAL(3,2) DEFAULT 0.00            COMMENT '评分 0-5',
  views         INT          DEFAULT 0,
  favorites     INT          DEFAULT 0,
  tags          JSON                               COMMENT '标签数组',
  teacher_id    BIGINT       DEFAULT NULL            COMMENT '教师发布者(预留 P3)',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_type (type),
  INDEX idx_status (status),
  INDEX idx_chapter (chapter_id, type),
  INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

-- 3.1.2 收藏表（从单体 z_archive_resource_favorites 迁移语义，独立建表）
CREATE TABLE resource_favorites (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  resource_id BIGINT      NOT NULL,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_resource (user_id, resource_id),
  INDEX idx_user_id (user_id),
  INDEX idx_resource_id (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源收藏表';

-- 3.1.3 反馈表（原单体无独立表，本期新增，支撑 P3 画像反馈闭环）
CREATE TABLE resource_feedback (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  resource_id BIGINT      NOT NULL,
  liked       TINYINT(1)  DEFAULT NULL,
  difficulty_feedback VARCHAR(50) DEFAULT NULL,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_resource (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源反馈表';
```

> 说明：`exercise_records` 与 `admin_stats_cache` 在主蓝图 §4.2 被列为 `resource_db`，但二者语义更贴近 learning/report 域。本期本服务**只建上面三张表**；`exercise_records`/`admin_stats_cache` 是否落户 `resource_db` 待 P3 与吴友诚/治理页确认（不阻塞 P1）。如确认归我，复用单体 `init.sql` 同名 DDL 迁入即可。

### 3.2 从现有单体表迁移
| 单体表 | 动作 | 映射要点 |
|--------|------|----------|
| `edu_agent.resources` | **迁移 → `learning_resources`** | `student_id → user_id`；`chapter_name → chapter`；`course_id → chapter_id`；新增 `prompt/ai_task_id/status/error_msg`（旧数据 `status` 默认 `published`，旧 `type` 值 video/article 保留但不在新生成枚举内）；去掉外键 `fk_resources_student`（DB-per-service 禁止跨库外键）。 |
| `edu_agent.z_archive_resource_favorites` | **迁移 → `resource_favorites`** | 直接字段映射，去跨库外键。 |
| 新 `resource_feedback` | 新建 | 无历史数据。 |
| `edu_agent.learning_resources`（init.sql 旧版） | 不迁移 | 旧版字段（completion_rate）与本服务无关，废弃。 |

迁移脚本（一次性，Flyway `V1_1__migrate_resources.sql` 或独立 `migrate.sql`）：
```sql
-- 仅结构迁移示例（生产用 INSERT ... SELECT 跨库，需同 MySQL 实例）
INSERT INTO resource_db.learning_resources
  (user_id, title, type, difficulty, chapter, chapter_id, course_name,
   description, content, status, rating, views, favorites, create_time, update_time)
SELECT student_id, title, type, difficulty, chapter_name, course_id, course_name,
       description, content, 'published', rating, views, favorites, create_time, update_time
FROM edu_agent.resources
WHERE student_id IS NOT NULL;
```

### 3.3 实体类（MyBatis-Plus）
```java
package com.eduagent.resource.entity;

@Data
@TableName("learning_resources")
public class Resource {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String title;
    private String type;
    private String difficulty;
    private String chapter;
    private String chapterId;
    private String courseName;
    private String description;
    private String content;       // JSON 或 Markdown 文本
    private String prompt;
    private String aiTaskId;
    private String status;        // generating|published|failed|archived
    private String errorMsg;
    private Double rating;
    private Integer views;
    private Integer favorites;
    private String tags;          // JSON
    private Long teacherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 3.4 Redis 缓存约定（key 前缀 `resource:*`，遵循蓝图 §3.4）
| Key | 结构 | TTL | 说明 |
|-----|------|-----|------|
| `resource:chapter:{chapterId}:{type}` | String(JSON `ResourceVO`) | 30 min | 热门章节资源；生成完成/更新时写入，删除/变更时 `DEL`。 |
| `resource:detail:{id}` | String(JSON `ResourceVO`) | 10 min | 详情缓存；views 计数异步，缓存可容忍短暂旧值。 |
| `resource:fav:{userId}` | Set(resourceId) | 1 h | 我的收藏集合，便于 2.2.8 秒回。 |

---

## 4. 关键实现

### 4.1 模块结构（Maven module：`resource-service`）
```
resource-service/
├── pom.xml                      # 依赖 common / openfeign / sentinel / rabbitmq / mybatis-plus
├── src/main/java/com/eduagent/resource/
│   ├── ResourceServiceApplication.java
│   ├── controller/ResourceController.java
│   ├── service/ResourceService.java
│   ├── service/impl/ResourceServiceImpl.java
│   ├── service/ResourceGenerator.java        # 调 ai + 回填（同步与 MQ worker 共用）
│   ├── mq/ResourceGenerateConsumer.java      # RabbitMQ 消费者(worker)
│   ├── mq/ResourceGenerateMessage.java        # 消息体
│   ├── client/AiResourceClient.java           # Feign → ai-service（含降级）
│   ├── client/LearningProfileClient.java     # Feign → learning-service（拉画像）
│   ├── client/fallback/AiResourceClientFallback.java
│   ├── config/RabbitConfig.java
│   ├── config/RedisConfig.java
│   └── entity/Resource.java  (Mapper/ResourceMapper.java, vo/ResourceVO.java)
└── src/main/resources/application.yml (或 Nacos 配置)
```

### 4.2 Controller（骨架）
```java
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping
    public Result<PageResult<ResourceVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return Result.success(resourceService.list(page, pageSize, keyword, type, status));
    }

    @GetMapping("/{id}")
    public Result<ResourceVO> getById(@PathVariable Long id) { return Result.success(resourceService.getDetail(id)); }

    @GetMapping("/chapter/{chapterId}")
    public Result<List<ResourceVO>> byChapter(@PathVariable String chapterId) {
        return Result.success(resourceService.listByChapter(chapterId));
    }

    // 命中缓存/已生成直接返；否则触发异步生成后返 generating 态
    @GetMapping("/chapter/{chapterId}/{type}")
    public Result<ResourceVO> chapterResource(
            @PathVariable String chapterId, @PathVariable String type,
            @RequestParam(defaultValue = "medium") String difficulty,
            @RequestParam(defaultValue = "") String chapterName,
            @RequestParam(defaultValue = "") String topic) {
        return Result.success(resourceService.getOrGenerate(chapterId, chapterName, topic, type, difficulty));
    }

    @PostMapping("/generate")
    public ResponseEntity<Result<ResourceVO>> generate(@RequestBody @Valid ResourceGenerateReq req) {
        ResourceVO vo = resourceService.generate(req);          // 建行+发MQ，或命中缓存直接返
        if ("generating".equals(vo.getStatus()))
            return ResponseEntity.status(202).body(Result.success(vo));
        return ResponseEntity.ok(Result.success(vo));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<Result<ResourceVO>> regenerate(@PathVariable Long id, @RequestBody Map<String,String> body) {
        ResourceVO vo = resourceService.regenerate(id, body.getOrDefault("difficulty","medium"));
        return ResponseEntity.status(202).body(Result.success(vo));
    }

    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, @RequestBody FavoriteReq req) {
        resourceService.favorite(id, req.getFavorite()); return Result.success();
    }

    @GetMapping("/favorites/mine")
    public Result<List<ResourceVO>> myFavorites() {
        return Result.success(resourceService.myFavorites(AuthContext.getUserId()));
    }

    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody FeedbackReq req) {
        resourceService.feedback(id, req); return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { resourceService.remove(id); return Result.success(); }
}
```
> 身份一律 `AuthContext.getUserId()`（P0 §6.2），不再读 SecurityContext/自己解析 JWT。

### 4.3 Service 核心逻辑
```java
public interface ResourceService {
    PageResult<ResourceVO> list(int page, int pageSize, String keyword, String type, String status);
    ResourceVO getDetail(Long id);
    List<ResourceVO> listByChapter(String chapterId);
    ResourceVO getOrGenerate(String chapterId, String chapterName, String topic, String type, String difficulty);
    ResourceVO generate(ResourceGenerateReq req);     // 返回 generating 或命中缓存
    ResourceVO regenerate(Long id, String difficulty);
    void favorite(Long id, boolean fav);
    List<ResourceVO> myFavorites(Long userId);
    void feedback(Long id, FeedbackReq req);
    void remove(Long id);
    // MQ worker 调用
    void doGenerate(Long resourceId);                 // 真正调 ai-service + 回填
}
```

`generate()` 关键流程（同步/异步分流）：
```java
@Override
public Result<ResourceVO> generate(ResourceGenerateReq req) {
    Long userId = AuthContext.getUserId();
    // 1) 非强制 && 已存在 published 同章节同类型同难度 → 直接返回（缓存命中语义）
    if (!req.isForce()) {
        Resource hit = findByChapterTypeDifficulty(req.getChapterId(), req.getType(), req.getDifficulty());
        if (hit != null && "published".equals(hit.getStatus()) && hasContent(hit))
            return Result.success(toVO(hit));
    }
    // 2) 建 generating 行
    Resource r = new Resource();
    r.setUserId(userId); r.setTitle(req.getChapterName()+" - "+typeLabel(req.getType()));
    r.setType(req.getType()); r.setDifficulty(req.getDifficulty());
    r.setChapter(req.getChapterName()); r.setChapterId(req.getChapterId());
    r.setCourseName(req.getChapterName()); r.setStatus("generating");
    r.setCreateTime(LocalDateTime.now()); r.setUpdateTime(LocalDateTime.now());
    resourceMapper.insert(r);
    // 3) 发 MQ 异步生成（幂等 key = resourceId）
    rabbitTemplate.convertAndSend("resource.generate", "resource.generate",
        new ResourceGenerateMessage(r.getId(), userId, req),
        m -> { m.getMessageProperties().setMessageId("res-"+r.getId()); return m; });
    return Result.success(toVO(r));   // status=generating
}
```

### 4.4 Feign → ai-service（封装：重试/超时/降级）
```java
@FeignClient(name = "ai-service",
    path = "/api/ai",
    configuration = FeignRetryConfig.class,           // 见 4.7
    fallbackFactory = AiResourceClientFallback.class) // 降级
public interface AiResourceClient {

    @PostMapping("/resource/generate")
    AiResourceGenResp generate(@RequestBody AiResourceGenReq req);

    @PostMapping("/path/generate")
    AiPathResp pathGenerate(@RequestBody AiPathReq req);
}

// 请求体（对齐 api.py ResourceGenRequest + 单体 AiClient.generateResource 字段）
@Data
public class AiResourceGenReq {
    private String studentId;     // 透传 AuthContext.getUserId()
    private String chapter;
    private String topic;
    private String resourceType;  // mindmap|quiz|reading|code|learning_path|...
    private String level;          // easy|medium|hard（对齐 api.py 的 level 字段）
    private String prompt;
    private Map<String,Object> profile;  // 来自 learning-service
}

// 响应体（对齐 api.py 返回的 {content, resourceType, chapter}）
@Data
public class AiResourceGenResp {
    private String content;        // LLM 原始输出（JSON/Markdown 文本）
    private String resourceType;
    private String chapter;
}
```

**降级（ai 不可用时给友好错误，不雪崩）**：
```java
@Component
public class AiResourceClientFallback implements FallbackFactory<AiResourceClient> {
    public AiResourceClient create(Throwable cause) {
        return new AiResourceClient() {
            public AiResourceGenResp generate(AiResourceGenReq req) {
                throw new BusinessException(502, "AI 服务暂不可用，请稍后重试");
            }
            public AiPathResp pathGenerate(AiPathReq req) {
                throw new BusinessException(502, "AI 服务暂不可用，请稍后重试");
            }
        };
    }
}
```
> `doGenerate()` 里 try/catch 调 Feign：捕获 `BusinessException`/超时 → 把资源行 `status` 置 `failed`、`errorMsg` 写原因，前端 2.2.2 友好展示重试。

### 4.5 Feign → learning-service（拉画像，只读）
```java
@FeignClient(name = "learning-service", path = "/api/learning")
public interface LearningProfileClient {
    @GetMapping("/profile")
    LearningProfileVO myProfile(@RequestHeader("X-User-Id") Long userId);
}
@Data public class LearningProfileVO {
    private String course; private String topic; private String knowledgeBase;
    private String weaknesses; private String pace; private String resourcePreference;
    private Integer lastScore;
}
```
> 画像字段沿用单体 `ResourceServiceImpl.loadStudentProfile` 的键（course/topic/knowledgeBase/weaknesses/pace/resourcePreference/lastScore），与 `LearningProfileVO` 一致（camelCase）。
> **联调风险**：learning-service 路径/字段名由陈海洋定，本文为提案契约；P1 初必须二人对齐（见 §6 契约测试）。

### 4.6 RabbitMQ 异步生成（对齐蓝图 §8 `resource.generate`）
```java
@Data @AllArgsConstructor @NoArgsConstructor
public class ResourceGenerateMessage implements Serializable {
    private Long resourceId;
    private Long userId;
    private ResourceGenerateReq req;   // 含 chapter/chapterName/topic/type/difficulty
}

// 消费者（同服务 worker）
@Component
@RequiredArgsConstructor
public class ResourceGenerateConsumer {
    private final ResourceGenerator generator;
    @RabbitListener(queues = "resource.generate.queue")
    public void onMessage(ResourceGenerateMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            generator.generate(msg.getResourceId(), msg.getReq()); // 调 ai + 回填
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // 有限次重试（MaxAttempts）→ 失败进 DLQ，资源置 failed
            throw e; // 交给 RabbitRetry 机制
        }
    }
}

// ResourceGenerator：调 ai + 写库 + 写缓存（同步接口与 worker 共用）
@Service @RequiredArgsConstructor
public class ResourceGenerator {
    private final AiResourceClient aiClient;
    private final LearningProfileClient profileClient;
    private final ResourceMapper resourceMapper;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void generate(Long resourceId, ResourceGenerateReq req) {
        Resource r = resourceMapper.selectById(resourceId);
        if (r == null || !"generating".equals(r.getStatus())) return; // 幂等
        Map<String,Object> profile = safe(() -> profileClient.myProfile(r.getUserId()));
        String prompt = buildPrompt(req, profile);  // 沿用单体 AiClient.buildResourcePrompt 逻辑
        AiResourceGenResp resp = aiClient.generate(new AiResourceGenReq(
                String.valueOf(r.getUserId()), req.getChapterName(), req.getTopic(),
                req.getType(), req.getDifficulty(), prompt, profile));
        r.setContent(resp.getContent());
        r.setPrompt(prompt);
        r.setStatus("published");
        r.setUpdateTime(LocalDateTime.now());
        resourceMapper.updateById(r);
        // 写缓存（热门章节）
        cacheChapterResource(r);
    }
}
```
> **幂等/去重**：`messageId="res-"+resourceId`（4.3 设置）+ 消费前校验 `status==generating`，双保险；失败按 `spring.rabbitmq.listener.simple.retry` 重试 3 次后进 DLQ（`resource.generate.dlq`）。

### 4.7 超时/重试/熔断配置（`application.yml` / Nacos）
```yaml
feign:
  circuitbreaker:
    enabled: true           # 启用 Sentinel/Resilience 熔断，接 fallbackFactory
  client:
    config:
      default:
        connect-timeout: 3000
        read-timeout: 60000  # ai 生成可能慢，给足 60s
spring:
  rabbitmq:
    host: rabbitmq
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000
        default-requeue-rejected: false   # 失败进 DLQ
  cloud:
    sentinel:
      enabled: true
```

### 4.8 与 learning-service 的关系
- **单向依赖**：resource-service → learning-service（只读画像 Feign）。禁止反向、禁止直连 `learning_db`。
- 画像缺失（learning 未就绪/异常）不阻断生成：`safe()` 捕获异常返回空 map，生成仍可用（降级为「无个性化」）。

---

## 5. 与陈海洋 ai-service 的对齐清单（防联调漂移）

> 陈海洋的 ai-service 子 spec 待出。以下契约以**主蓝图 §6.4** + **现有 `edu-agent-ai/api.py`** 推导，resource-service 严格依赖这些端点/字段。任何一项变动必须同步通知陈嘉成。

### 5.1 我要调的 ai-service 端点（Feign 目标 `ai-service:8001`）
| 我的用途 | ai-service 端点 | 方法 | 现状（api.py） | 行动 |
|----------|----------------|------|----------------|------|
| 生成思维导图/题库/阅读/代码/路径 | `/api/ai/resource/generate` | POST | api.py 现是 `/resource/generate`，返回 `{content, resourceType, chapter}` | **陈海洋需把路由挂到 `/api/ai` 前缀下**（与蓝图 §6.4 一致）；响应字段 `content` 必须保留。 |
| 学习路径生成 | `/api/ai/path/generate` | POST | api.py 现是 `/path/generate`，返回 learning_path JSON | 同上，加 `/api/ai` 前缀。 |

> ⚠️ **漂移风险点（必须对齐）**：api.py 当前路径是 `/resource/generate`、`/path/generate`、`/chat`，**没有 `/api/ai` 前缀**。主蓝图 §6.4 与网关路由 §5.2 要求对外是 `/api/ai/**`。两种落地方案，二选一，但**必须全员一致**：
> - 方案 A（推荐，贴蓝图）：ai-service 在 FastAPI 上挂 `app.include_router(..., prefix="/api/ai")`，Feign 直连 `ai-service:8001/api/ai/resource/generate`。
> - 方案 B：ai-service 保持原路径，由 Gateway 把 `/api/ai/**` strip 前缀后转发到 `/**`。但 Feign 是**服务间直连**（不经网关），所以 Feign 仍要调真实路径 → 那 resource-service 的 `@FeignClient(path="/api/ai")` 就会打错。
> **结论**：采用方案 A。陈海洋在 ai-service 加 `/api/ai` 前缀；我的 Feign `path="/api/ai"` 不变。**此条写进双方联调 check list。**

### 5.2 请求字段逐条对齐（→ `AiResourceGenReq`）
| 字段 | 类型 | 来源 | api.py 对应 | 备注 |
|------|------|------|-------------|------|
| `student_id` | string | `AuthContext.getUserId()` | `ResourceGenRequest.student_id` | 透传，ai 用于画像/日志。 |
| `chapter` | string | `req.chapterName` | `chapter` | 章节名。 |
| `topic` | string | `req.topic` | `topic` | 知识点。 |
| `resourceType` | string | `req.type` | `resourceType` | 枚举（生成产物类型，与陈海洋 §1.3.2 一致）：**mindmap/quiz/reading/code/learning_path**。`suggestion/judge/evaluation/review/summary/explain` 属 ai-service 的 `mode` 轴（见《契约对齐决议》C3），非本字段取值。 |
| `level` | string | `req.difficulty` | `level` | 取值与 ai 一致：`basic`（陈海洋 §1.3.2 `easy→basic`）；我侧 easy/medium/hard → basic/intermediate/advanced 映射后透传。 |
| `prompt` | string | 我本地 `buildPrompt()` 拼装 | `prompt`（api.py 直接用） | 沿用单体 `AiClient.buildResourcePrompt` 的 JavaSE 限定 + 画像拼接逻辑。 |
| `profile` | object | learning-service 画像 | api.py 未用此字段，但单体 `AiClient.generateResource` 有传 | **新增协商字段**：建议 ai-service 接收后注入 prompt，使生成个性化；若陈海洋暂不支持，我侧把 profile 拼进 `prompt` 即可，不阻塞。 |

### 5.3 响应字段逐条对齐（← `AiResourceGenResp`）
| 字段 | 类型 | 必返回 | api.py 对应 | 我的处理 |
|------|------|--------|-------------|----------|
| `content` | string | ✅ 必须 | `result["content"]` | 直接存 `Resource.content`。可能是 JSON 串（mindmap/quiz）或 Markdown（reading/code）。 |
| `resourceType` | string | 可选 | `resourceType` | 回写校验。 |
| `chapter` | string | 可选 | `chapter` | 回写校验。 |

> 降级契约：ai-service 不可达/超时 → 我的 Feign fallback 返回 502 友好错误（§4.4），资源行 `status=failed`，**不抛 5xx 给前端造成雪崩**。

### 5.4 联调前双方确认项（checklist）
- [ ] ai-service 路由已加 `/api/ai` 前缀（方案 A）。
- [ ] `/api/ai/resource/generate` 入参接受 `student_id/chapter/topic/resourceType/level/prompt/profile`。
- [ ] 响应稳定返回 `{content, resourceType, chapter}`，`content` 永不空（空则 ai 侧给占位文本，不让我侧判空失败）。
- [ ] 超时阈值一致：我侧 read-timeout=60s，ai 侧单请求 P99 < 50s。
- [ ] `USE_MOCK_LLM=1` 降级开关可用（蓝图 §6.5），联调期我侧可先打 mock 跑通链路。
- [ ] health 端点 `/api/ai/health`（或 `/health`）供我侧与网关探活。

---

## 6. 测试

### 6.1 单元测试（JUnit5 + Mockito）
- `ResourceServiceImpl.generate()`：
  - 命中缓存分支（已存在 published）→ 不发 MQ，直接返回命中行。
  - 未命中 → 插入 generating 行 + `rabbitTemplate.convertAndSend` 被调用 1 次（Mock RabbitTemplate 验证）。
- `ResourceGenerator.generate()`：Mock `AiResourceClient` 返回 content → 验证 `status` 变 `published`、content 写入、Redis 缓存写入。
- **Feign 降级单测**：Mock `AiResourceClient` 抛异常 → 验证 fallback 抛 `BusinessException(502,...)`；`ResourceGenerator` 捕获后把行置 `failed` + `errorMsg`。
- `buildPrompt()`：验证 JavaSE 限定语与画像字段拼接正确（沿用单体逻辑回归）。

### 6.2 集成测试（Testcontainers）
- 起 `MySQLContainer` + `RabbitMQContainer` + `GenericContainer(Redis)`，启动 resource-service（`@SpringBootTest`），桩 `AiResourceClient`（`@MockBean`）与 `LearningProfileClient`：
  - 发 `POST /api/resource/generate` → 断言 202 + `status=generating`。
  - 直接调用 `/api/resource/{id}` 轮询或直接调 `ResourceGenerator.generate()` → 断言 `status=published`、DB 行更新、Redis 有缓存。
  - 断言 `DELETE` 越权（换 user）返回 403。
- 可选真实 ai-service：P1 联调期用 `Testcontainers` 起 `ai-service`（或 `USE_MOCK_LLM=1` 容器）做端到端冒烟。

### 6.3 契约测试（防与 ai 漂移，蓝图 §11.3）
- **Spring Cloud Contract（推荐，Java 侧）**：在 resource-service 写契约 `resource_generation.yml`，定义 `POST /api/ai/resource/generate` 的 request/response 形态 → 生成 stub 供我侧集成测试；同时把契约发布给 ai-service 侧做 `verifier`（陈海洋实现 ai-service 时用此契约验证，保证不漂移）。
- 备选 **Pact**：我侧作为 consumer 发 Pact 文件，ai-service 作 provider 验证。
- 前端（曾姿妍）按我的 OpenAPI（`/v3/api-docs`）生成契约，三方对齐。

### 6.4 质量门（CI）
- Checkstyle / PMD / SpotBugs 作为构建门（主蓝图 §11.1），不达标阻断 PR。
- 覆盖率：Service 层 ≥ 70%。

---

## 7. 验收标准（Definition of Done）

### 7.1 P1 交付（学生纵切）
- [ ] `resource-service` 模块编译通过，被 `common` 依赖，`spring.application.name=resource-service`，端口 8083，注册到 Nacos。
- [ ] 网关 `/api/resource/**` 路由到本服务；受保护接口无 token → 401（网关拦），有 token → 透传 `X-User-Id` 可用 `AuthContext.getUserId()` 取到（两跳透传验证，复用 P0 验证脚本）。
- [ ] 资源列表/详情/按章节查询/收藏/点赞/删除 全部跑通，数据来自 `resource_db`（不连其他库）。
- [ ] `POST /generate` 异步 202：建 generating 行 → 发 `resource.generate` MQ → worker 调 ai-service → 回填 `published`；前端轮询可见状态流转。
- [ ] Redis 缓存热门章节资源生效，详情命中缓存；删除/更新清缓存。
- [ ] ai-service 不可用时降级：资源行 `failed` + 友好错误，前端可重试，**不雪崩**（Sentinel/Feign fallback 生效）。
- [ ] 与陈海洋的 ai-service 联调通过（§5.4 checklist 全勾），`/api/ai` 前缀一致。
- [ ] 与 learning-service 画像 Feign 打通（或安全降级为空画像不阻断）。
- [ ] 单测 + 集成（Testcontainers）绿；契约（Spring Cloud Contract）基线建立。

### 7.2 P2 支援项（可支援 ai/code）
- [ ] 若吴友诚需要，`code` 类资源生成链路已可用（我生成「教学代码片段文本」，沙箱归 code-service）。
- [ ] `resource_db` 迁移脚本可用，旧 `edu_agent.resources` 数据无损迁入。

### 7.3 文档与演示
- [ ] 本 spec 落 git，OpenAPI 文档（`/v3/api-docs`）可被前端（曾姿妍）消费。
- [ ] 演示脚本：学生登录 → 进章节 → 生成思维导图/题库/阅读 → 轮询到 published → 收藏/点赞 → 看 Redis 命中。

---

## 8. 知识库基础语料采集与清洗流水线（RAG 语料 · 陈嘉成端到端负责）

> 背景：RAG 检索质量的上限由语料决定（data-centric AI）。陈海洋的 ai-service **只负责 embed + Chroma 检索**，不碰原始语料。**基础语料的寻找、采集、清洗、去重、分块、落库由本服务（resource-service / 陈嘉成）端到端负责**，清洗后的干净语料再交付 ai-service 的 `/api/ai/kb/rebuild` 做向量化。归属见主蓝图 §6.2。

### 8.1 数据源
- 教材/课件：JavaSE 基础（语法 / 面向对象 / 集合 / IO / 多线程 / 反射 / 异常等）的 PDF / MD / Word。
- 课堂笔记：既有 `edu_agent.java_notes` 表（如有）、教师上传的讲义。
- 官方文档：Oracle Java Docs、W3Cschool 等结构化抓取（限定白名单域名）。
- 本服务既有资源：已生成的 mindmap / quiz / reading / code 文本，作为补充语料。

### 8.2 采集与导入
- 文件导入：后台上传接口（仅 ADMIN / 教师）→ 落 `kb_raw_docs`。
- DB 导入：从 `java_notes` 等表读取 → 转纯文本。
- 网页抓取：限定白名单域名，避免噪声灌入。

### 8.3 清洗规则
- 去除广告 / 导航 / 页脚 / 版权等模板噪声。
- 格式归一：MD / HTML → 纯文本；统一换行、全角半角、编码 UTF-8。
- 代码块保真：代码块不拆行、保留缩进。
- 质量过滤：过短（< 50 字）/ 纯目录 / 重复页剔除。

### 8.4 去重策略
- **精确去重**：内容归一化后计算 SHA-256，哈希相同则丢弃。
- **语义去重**（P2 引入）：embedding 后余弦相似度 > 0.95 归并。
- **元数据去重**：同一 `(source, chapter, version)` 只保留最新。

### 8.5 分块策略（chunking）
- 优先按章节结构切分（H1 / H2 / H3 为边界）。
- 兜底固定窗口：~800 token / 段，重叠 100 token 保上下文。
- 每段附带元数据：`source / chapter / section / version / seq`。

### 8.6 存储（清洗后语料落库）
新增 `kb_corpus` 表（本期放 `resource_db`）：
```sql
CREATE TABLE kb_corpus (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source VARCHAR(100) NOT NULL COMMENT '来源：java_notes/教材/官方文档',
  chapter VARCHAR(100),
  section VARCHAR(100),
  version VARCHAR(32) DEFAULT 'v1',
  content LONGTEXT NOT NULL COMMENT '清洗后纯文本（一个 chunk）',
  content_hash CHAR(64) COMMENT 'SHA-256 精确去重',
  token_count INT,
  status TINYINT DEFAULT 0 COMMENT '0 待向量化 1 已向量化',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_hash (content_hash),
  INDEX idx_source_chapter (source, chapter),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 清洗后语料';
```
> 与 ai-service 的边界：本表只存清洗后文本 + 元数据；向量（Chroma）由 ai-service 写入。本服务**不连 Chroma**。

### 8.7 与 ai-service 的对接契约（依《契约对齐决议》C6 定稿）
- 触发：管理端 / 教师触发 `POST /api/ai/kb/rebuild`（陈海洋实现，入参 `{}` 或 `{collection, force}` 不变）；语义改为「从 resource-service 拉取 `kb_corpus`(status=0) → embed → 写 Chroma(collection) → 回调 `mark-indexed`」。
- 语料权威源 = `kb_corpus`（本服务清洗流水线产出）；陈海洋的 `java_notes` 本地 md 仅作开发期种子/兜底，不进生产向量化主链路。
- 交付方式（**已定稿，采用方式 B，避免 ai 直连库**）：
  - ai-service 经 `GET /api/resource/kb/corpus?status=0` 拉取待向量化语料（本服务提供，ai 不直连库）；
  - ai-service 嵌入写 Chroma 后，**回调** `POST /api/resource/kb/mark-indexed`（body `{ids:[Long], collection}`，见 §8.7.1）通知本服务把对应行 `status` 置 `1`；
  - **明确硬约束**：ai-service **不连、不写任何 MySQL 关系表（含 `resource_db`）**——呼应陈海洋 §1.4.1 的 DB-per-service 硬约束。`kb_corpus.status` 由本服务（resource-service）维护，ai 不在自己侧维护任何关系表。
- 由此避免重复嵌入：本服务在收到 `mark-indexed` 回调后，把对应行 `status` 回写为 `1`。

#### 8.7.1 回调端点：`POST /api/resource/kb/mark-indexed`（供 ai-service 向量化完成后回调）
- **角色**：A（ADMIN）。仅 ai-service 在向量化完成后经内网 / 网关白名单回调。
- **请求体**：`{ "ids": [Long], "collection": "String" }`（`ids` 为 `kb_corpus` 中待置位行 id；`collection` 为 Chroma 集合名，用于对账）。
- **作用**：把 `kb_corpus` 中对应 `id` 的行 `status` 置 `1`（已向量化）。本服务写库，**ai 不碰任何关系表**。
- **幂等**：重复回调同批 id 安全（已 `status=1` 的行保持不变）。
- **呼应**：本端点是 ai-service 唯一的「反向通知」入口；ai 不连、不写 `resource_db`，满足 DB-per-service 硬约束。

### 8.8 质量校验（DoD）
- 覆盖率：JavaSE 各章节均有语料（缺失章节告警）。
- 重复率：精确去重后重复 < 1%。
- 章节完整性：每个 chapter 至少 N 个 chunk。
- 端到端命中：触发 `/api/ai/kb/rebuild` 后，检索 top-k 能命中对应章节。

### 8.9 新增端点（ADMIN / 教师）
| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| GET | `/api/resource/kb/corpus` | 语料列表 / 查询（含 status），ai-service 经 `?status=0` 拉取待向量化语料 | A / T |
| POST | `/api/resource/kb/mark-indexed` | ai-service 向量化完成后回调，把 `kb_corpus` 对应行 `status` 置 `1`（body `{ids:[Long], collection}`） | A |
| POST | `/api/resource/kb/import` | 触发一次采集 + 清洗流水线（文件 / DB 源） | A |
| POST | `/api/resource/kb/rebuild` | 转发 / 触发 ai-service `/api/ai/kb/rebuild` | A |
| GET | `/api/resource/kb/stats` | 覆盖率 / 重复率 / 章节完整性统计 | A |

---

## 附录 A：关键配置片段（`application.yml` 节选）
```yaml
server:
  port: 8083
spring:
  application:
    name: resource-service
  datasource:
    url: jdbc:mysql://mysql:3306/resource_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PWD}
  rabbitmq:
    host: rabbitmq
    port: 5672
  data:
    redis:
      host: redis
      port: 6379
mybatis-plus:
  mapper-locations: classpath*:/mapper/*.xml
  global-config:
    db-config:
      logic-delete-field: deleted
cloud:
  nacos:
    discovery:
      server-addr: nacos:8848
    config:
      server-addr: nacos:8848
      namespace: edu-dev
      group: resource-group
```

## 附录 B：依赖 P0 约定（照抄，不重新发明）
- `AuthContext.getUserId()` / `getRoles()` 取身份；`AuthContextFilter` + `AuthFeignInterceptor` 由 `common` 提供，本服务启动类加 `@EnableFeignClients` + 引入 `common` 自动配置即可。
- 统一 `Result` / `PageResult` / `BusinessException` / `GlobalExceptionHandler` 来自 `common`，Controller 直接返回 `Result.success(...)`。
- JWT 密钥/算法与 Gateway 一致（Nacos 下发），本服务**不持有 JWT 解析逻辑**（仅消费网关注入头）。

---

*resource-service 子 spec 结束。契约以 §5 与陈海洋 ai-service / 陈海洋 learning-service 对齐为准，P1 评审后定稿落 git。*
