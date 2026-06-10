# AI 资源生成功能 — 实现文档

> 更新时间：2026-06-06

## 一、功能概述

在课程学习页面（`CourseDetail.vue`）的右侧面板中，为每个章节提供 **4 种 AI 智能生成的配套资源**：

| 资源类型 | 图标 | 说明 |
|---------|------|------|
| 思维导图 | 🧠 | Mermaid 格式的知识结构图 |
| 练习题目 | 📝 | 含答案和解析的选择题/简答题 |
| 拓展阅读 | 📖 | 进阶概念、应用场景、推荐学习方向 |
| 代码案例 | 💻 | 可运行的 Java 代码（含详细注释） |

### 核心特性

- **画像分析驱动**：生成时传入学生画像（薄弱点、知识基础、学习节奏），实现个性化内容
- **难度可调**：支持"更简单 / 刚好 / 更困难"三档难度，切换即重新生成
- **DB-First 架构**：先查数据库缓存 → 无则调 AI 生成 → 存入数据库 → 返回前端
- **章节绑定**：每个资源关联章节 ID，切换章节自动加载对应资源
- **反馈收集**：👍/👎 按钮记录用户偏好

---

## 二、架构设计

### 数据流

```
┌──────────┐    GET /api/resources/chapter/{id}/{type}    ┌──────────┐
│  前端     │ ──────────────────────────────────────────→ │  后端     │
│ Vue3     │ ←────────────────────────────────────────── │  Spring   │
│          │        返回 Resource JSON                    │  Boot     │
└──────────┘                                             └────┬─────┘
                                                              │
                                                    ┌─────────┴─────────┐
                                                    │                   │
                                              有缓存？              无缓存？
                                                    │                   │
                                                    ▼                   ▼
                                            ┌──────────┐     ┌──────────────┐
                                            │  MySQL   │     │  Python AI   │
                                            │ resources│     │  /resource/  │
                                            │  表      │     │  generate    │
                                            └──────────┘     └──────┬───────┘
                                                                   │
                                                    ┌──────────────┘
                                                    │ 保存到 resources 表
                                                    ▼
                                            ┌──────────┐
                                            │  返回前端  │
                                            └──────────┘
```

### 难度切换流程

```
POST /api/resources/{id}/regenerate  { difficulty: "easy"|"medium"|"hard" }
  → 读学生画像 → 调 AI (新难度 prompt) → 更新 DB 记录 → 返回新内容
```

---

## 三、修改文件清单

### 3.1 Python AI 智能体

**文件**: `edu-agent-ai/api.py`

扩展了 `ResourceGenRequest` 模型，新增字段：

```python
class ResourceGenRequest(BaseModel):
    difficulty: str = "medium"        # easy | medium | hard
    profile: Optional[dict] = None    # 学生画像
    chapter_id: Optional[str] = None
    course_id: Optional[str] = None
    student_id: str = "student_001"
```

新增 `_build_resource_prompt()` 函数，根据资源类型、难度、画像生成个性化 prompt：

- `easy` → 简化语言，减少术语，适合零基础
- `medium` → 标准教学语言
- `hard` → 深入技术细节和底层原理

画像信息（薄弱点、知识基础、学习节奏）直接嵌入 prompt，影响生成内容。

### 3.2 后端 (Spring Boot)

#### `AiClient.java` — 新增方法

```java
public Map<String, Object> generateResource(
    String studentId, String chapter, String topic,
    String resourceType, String difficulty,
    Map<String, Object> profile)
```

调用 `POST http://localhost:8000/resource/generate`，带回退处理和错误日志。

#### `ResourceService.java` — 新增接口

```java
Resource getByChapterId(Long chapterId, String type);
List<Resource> listByChapterId(Long chapterId);
Resource generateResource(Long chapterId, String chapterName, String topic, String type, String difficulty, Long studentId);
Resource regenerateResource(Long resourceId, String difficulty, Long studentId);
void saveFeedback(Long resourceId, Boolean liked, String difficultyFeedback);
Resource findByChapterAndType(Long chapterId, String type);
```

#### `ResourceServiceImpl.java` — 完整实现

核心逻辑：

1. **缓存优先**：先查 `resources` 表中同章节+同类型+同难度的记录
2. **画像加载**：从 `student_profiles` 表读取学生画像（薄弱点、知识基础、学习节奏）
3. **AI 生成**：调用 `AiClient.generateResource()` 获取内容
4. **DB 持久化**：插入 `resources` 表（title, type, difficulty, content, course_id 等）
5. **重生成**：更新现有记录而非新建

#### `ResourceController.java` — 完整端点

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/resources` | 分页列表（keyword/type/status 筛选） |
| GET | `/api/resources/{id}` | 单个资源详情（含浏览量+1） |
| GET | `/api/resources/chapter/{chapterId}` | 章节全部资源 |
| GET | `/api/resources/chapter/{chapterId}/{type}` | 章节特定类型（有缓存返回，无则生成） |
| POST | `/api/resources/generate` | 主动触发 AI 生成 |
| POST | `/api/resources/{id}/regenerate` | 换难度重新生成 |
| POST | `/api/resources/{id}/feedback` | 保存用户反馈 |
| DELETE | `/api/resources/{id}` | 删除资源 |

### 3.3 前端 (Vue 3)

#### `api/resource.ts` — API 层重构

从直连 AI（`/ai/resource/generate`）改为通过后端：

```typescript
// 新增函数
getChapterResources(chapterId)    // 获取章节全部资源
getChapterResource(id, type, ...) // 获取章节特定类型
generateResource(params)          // 主动生成
regenerateResource(id, diff)      // 重新生成
saveResourceFeedback(id, fb)     // 保存反馈
```

#### `CourseDetail.vue` — 右侧面板改造

**原有面板**：课程任务 + 学习建议  
**改造后**：AI 学习资源面板 + 课程任务

新增资源面板组件结构：

```
📚 AI 学习资源
├── [🧠 思维导图] [📝 练习题目]
├── [📖 拓展阅读] [💻 代码案例]
├── 内容展示区
│   ├── 思维导图 → <pre> 文本
│   ├── 练习题目 → 题目卡片 + 选项 + 显示答案
│   ├── 拓展阅读 → HTML 渲染
│   └── 代码案例 → <pre> 代码
├── 难度：◉更简单  ○刚好  ○更困难
└── 👍  👎  内容有帮助吗？
```

**交互逻辑**：

```typescript
// 切换资源标签 → 加载对应资源
switchResourceTab(type) → loadChapterResource()

// 切换章节 → 自动加载
watch(currentChapter) → loadChapterResource()

// 切换难度 → 调用重新生成接口
onDifficultyChange(difficulty) → regenerateResource(id, difficulty)
```

- 所有原有样式（`.course-hero`, `.chapter-panel`, `.learning-layout`, `.side-panel` 等）保持不变
- 新增样式使用相同的设计变量（border-radius、颜色、间距）

---

## 四、数据库

### resources 表（已存在的表结构，复用）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| title | VARCHAR | 资源标题（如 "第1章：Java基础语法 - 思维导图"） |
| type | VARCHAR | 资源类型：mindmap / quiz / reading / code |
| difficulty | VARCHAR | 难度：easy / medium / hard |
| content | TEXT | AI 生成的内容 |
| course_id | VARCHAR | 关联章节 ID |
| course_name | VARCHAR | 章节名称 |
| status | VARCHAR | 状态：published |
| views | INT | 浏览次数 |
| favorites | INT | 收藏次数 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### student_profiles 表（已存在，用于画像读取）

生成资源时读取以下字段传入 AI：
- `course` — 课程名
- `topic` — 当前知识点
- `knowledge_base` — 知识基础
- `weaknesses` — 薄弱点
- `pace` — 学习节奏
- `resource_preference` — 资源偏好

---

## 五、启动与验证

### 开发环境启动

```bash
# 1. 启动 Python AI 智能体（端口 8000）
cd edu-agent-ai
pip install -r requirements.txt
python run_demo.py

# 2. 启动 Spring Boot 后端（端口 8080）
# 方式一：命令行
start-backend.bat

# 方式二：IDEA
# 运行 EduAgentApplication.java

# 3. 启动前端（端口 5173）
cd edu-agent-web
npm install
npm run dev
```

### 功能验证步骤

1. 浏览器访问 `http://localhost:5173`
2. 登录（学生账号）
3. 左侧菜单 → 点击"资源生成"或进入课程中心
4. 进入课程 → 选择章节
5. **右侧面板**出现 `📚 AI 学习资源` 卡片
6. 点击 `🧠 思维导图` → 看到 AI 生成的 Mermaid 代码
7. 点击 `📝 练习题目` → 看到题目卡片，可显示答案
8. 切换难度到"更困难" → 内容重新生成
9. 切换章节 → 资源自动加载对应章节内容
10. 点击 👍/👎 → 反馈已记录

### 数据库验证

```sql
-- 查看已生成的资源
SELECT id, title, type, difficulty, course_id, views, create_time
FROM resources
ORDER BY create_time DESC;

-- 查看某章节的所有资源
SELECT * FROM resources WHERE course_id = '1';
```

---

## 六、相关文件索引

| 层 | 文件路径 | 角色 |
|----|---------|------|
| AI | `edu-agent-ai/api.py` | `/resource/generate` 端点 |
| AI | `edu-agent-ai/school_agent/services/llm_client.py` | LLM 调用封装 |
| AI | `edu-agent-ai/school_agent/agents/resource_agent.py` | 资源生成智能体 |
| 后端 | `edu-agent-server/.../controller/ResourceController.java` | REST API |
| 后端 | `edu-agent-server/.../service/ResourceService.java` | 业务接口 |
| 后端 | `edu-agent-server/.../service/impl/ResourceServiceImpl.java` | 业务实现 |
| 后端 | `edu-agent-server/.../agent/AiClient.java` | AI 调用客户端 |
| 后端 | `edu-agent-server/.../entity/Resource.java` | 数据实体 |
| 前端 | `edu-agent-web/src/views/student/CourseDetail.vue` | 课程详情页（右侧面板） |
| 前端 | `edu-agent-web/src/api/resource.ts` | 资源 API 层 |
| 前端 | `edu-agent-web/src/api/course.ts` | 课程 API（类型定义） |
| 前端 | `edu-agent-web/vite.config.ts` | 代理配置 |
| 配置 | `edu-agent-web/.env.development` | 环境变量 |
