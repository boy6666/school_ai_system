# EduAgent 数据库设计文档

## 概述

EduAgent 数据库设计支持智能教育平台的所有功能，包括用户管理、学生画像、学习资源、任务管理、练习系统、智能辅导等核心功能。

**数据库信息**:
- 数据库名称: `edu_agent`
- 字符集: `utf8mb4_unicode_ci`
- 表数量: 18个核心表
- 索引策略: 基于查询优化的复合索引

---

## 数据库结构总览

### 1. 用户相关模块
- `users` - 用户基础信息表
- `roles` - 角色权限表

### 2. 学生画像模块
- `student_profiles` - 学生画像表

### 3. 学习资源模块
- `resources` - 学习资源表
- `resource_chapters` - 资源章节表
- `resource_reviews` - 资源评价表
- `resource_favorites` - 资源收藏表
- `learning_plans` - 学习计划表

### 4. 学习任务模块
- `learning_tasks` - 学习任务表

### 5. 练习系统模块
- `practice_questions` - 练习题表
- `practice_records` - 练习记录表

### 6. 智能辅导模块
- `tutor_sessions` - 辅导会话表
- `tutor_messages` - 辅导消息表
- `tutor_suggestions` - 辅导建议表

### 7. 学习分析模块
- `learning_records` - 学习记录表
- `learning_reports` - 学习报告表

### 8. 管理后台模块
- `admin_logs` - 管理员操作日志表
- `system_settings` - 系统设置表

### 9. 其他模块
- `messages` - 消息通知表
- `projects` - 项目管理表
- `courses` - 课程管理表

---

## 核心表详细说明

### users (用户表)

**用途**: 存储所有用户的基础信息

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 用户ID (主键) | PRIMARY |
| username | VARCHAR(50) | 用户名 (唯一) | UNIQUE |
| password | VARCHAR(255) | 密码 (加密) | - |
| nickname | VARCHAR(100) | 昵称 | - |
| email | VARCHAR(100) | 邮箱 | INDEX |
| phone | VARCHAR(20) | 手机号 | - |
| avatar | VARCHAR(255) | 头像URL | - |
| role | ENUM | 角色: student/teacher/admin | INDEX |
| status | ENUM | 状态: active/inactive/locked | INDEX |
| last_login_time | DATETIME | 最后登录时间 | - |
| last_login_ip | VARCHAR(50) | 最后登录IP | - |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

**示例数据**:
```sql
INSERT INTO users (username, password, nickname, email, role, status)
VALUES ('student001', '加密密码', '张三', 'student@example.com', 'student', 'active');
```

---

### student_profiles (学生画像表)

**用途**: 存储学生的个性化学习画像信息

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 画像ID (主键) | PRIMARY |
| student_id | BIGINT | 学生ID (外键) | UNIQUE |
| major | VARCHAR(100) | 专业 | - |
| grade | VARCHAR(50) | 年级 | - |
| course | VARCHAR(100) | 课程 | - |
| topic | VARCHAR(100) | 当前学习主题 | INDEX |
| learning_goal | TEXT | 学习目标 | - |
| knowledge_base | TEXT | 知识基础 | - |
| cognitive_style | TEXT | 认知风格 | - |
| pace | VARCHAR(50) | 学习节奏 | - |
| weaknesses | JSON | 薄弱点 (数组) | - |
| mistake_patterns | JSON | 易错模式 (数组) | - |
| resource_preference | JSON | 资源偏好 (数组) | - |
| overall_type | ENUM | 整体类型: 基础补齐型/稳定提升型/进阶拓展型 | INDEX |
| profile_suggestions | JSON | 画像建议 (数组) | - |
| last_score | INT | 最近评估分数 | - |
| last_suggestion | TEXT | 最近建议 | - |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

**JSON字段示例**:
```json
{
  "weaknesses": ["递归", "二叉树"],
  "mistake_patterns": ["概念混淆", "边界条件遗漏"],
  "resource_preference": ["讲解文档", "代码案例", "练习题"],
  "profile_suggestions": [
    "建议围绕薄弱点进行查漏补缺。",
    "建议增加变式练习，提升知识迁移能力。"
  ]
}
```

---

### resources (学习资源表)

**用途**: 存储各种学习资源信息

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 资源ID (主键) | PRIMARY |
| title | VARCHAR(200) | 资源标题 | - |
| type | ENUM | 资源类型: 文档/PPT/视频/动画/题库/代码案例/实验项目/拓展阅读/思维导图 | INDEX |
| difficulty | ENUM | 难度: 入门/基础/进阶/高级 | INDEX |
| description | TEXT | 资源描述 | - |
| content | LONGTEXT | 资源内容 | - |
| file_url | VARCHAR(500) | 文件URL | - |
| file_size | VARCHAR(50) | 文件大小 | - |
| cover | VARCHAR(255) | 封面图片 | - |
| author | VARCHAR(100) | 作者 | - |
| rating | DECIMAL(3,2) | 评分 (0-5) | INDEX |
| views | INT | 浏览量 | INDEX |
| favorites | INT | 收藏数 | - |
| duration | VARCHAR(50) | 时长 | - |
| course_id | VARCHAR(50) | 课程ID | INDEX |
| course_name | VARCHAR(100) | 课程名称 | - |
| chapter_name | VARCHAR(100) | 章节名称 | - |
| chapter_count | INT | 章节数量 | - |
| tags | JSON | 标签 (数组) | - |
| goals | JSON | 学习目标 (数组) | - |
| suitable_for | JSON | 适合人群 (数组) | - |
| status | ENUM | 状态: draft/published/archived | INDEX |
| teacher_id | BIGINT | 教师ID (外键) | - |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

**特殊索引**:
- 全文索引: `idx_content` (title, description, content) 支持全文搜索

---

### learning_tasks (学习任务表)

**用途**: 管理学生的学习任务

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 任务ID (主键) | PRIMARY |
| user_id | BIGINT | 用户ID (外键) | INDEX |
| title | VARCHAR(200) | 任务标题 | - |
| description | TEXT | 任务描述 | - |
| course_name | VARCHAR(100) | 课程名称 | - |
| chapter_name | VARCHAR(100) | 章节名称 | - |
| start_time | DATETIME | 开始时间 | - |
| end_time | DATETIME | 结束时间 | INDEX |
| priority | ENUM | 优先级: high/middle/low | INDEX |
| status | ENUM | 状态: todo/doing/done | INDEX |
| progress | INT | 进度 (0-100) | - |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

---

### practice_questions (练习题表)

**用途**: 存储各种练习题

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 题目ID (主键) | PRIMARY |
| type | ENUM | 题目类型: 选择题/判断题/填空题/简答题/代码题 | INDEX |
| difficulty | ENUM | 难度: 入门/基础/进阶/高级 | INDEX |
| topic | VARCHAR(100) | 主题 | INDEX |
| course_id | VARCHAR(50) | 课程ID | INDEX |
| chapter | VARCHAR(100) | 章节 | - |
| question | TEXT | 题目内容 | - |
| options | JSON | 选项 (数组，选择题用) | - |
| answer | TEXT | 正确答案 | - |
| analysis | TEXT | 解析 | - |
| tags | JSON | 标签 (数组) | - |
| score | INT | 分值 | - |
| teacher_id | BIGINT | 出题人ID (外键) | - |
| status | ENUM | 状态: draft/published/archived | INDEX |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

**JSON字段示例**:
```json
{
  "options": [
    "参数和返回值",
    "基准情况（终止条件）和递归调用",
    "变量和常量",
    "循环和条件"
  ],
  "tags": ["递归", "算法", "基础"]
}
```

---

### practice_records (练习记录表)

**用途**: 记录学生的练习历史

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 记录ID (主键) | PRIMARY |
| user_id | BIGINT | 用户ID (外键) | INDEX |
| question_id | BIGINT | 题目ID (外键) | INDEX |
| user_answer | TEXT | 用户答案 | - |
| is_correct | TINYINT(1) | 是否正确 | - |
| score | INT | 得分 | - |
| time_spent | INT | 用时 (秒) | - |
| practice_time | DATETIME | 练习时间 | INDEX |
| create_time | DATETIME | 创建时间 | - |

---

### tutor_sessions (辅导会话表)

**用途**: 管理智能辅导的会话

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 会话ID (主键) | PRIMARY |
| user_id | BIGINT | 用户ID (外键) | INDEX |
| session_id | VARCHAR(100) | 会话标识 (唯一) | UNIQUE |
| title | VARCHAR(200) | 会话标题 | - |
| topic | VARCHAR(100) | 主题 | - |
| start_time | DATETIME | 开始时间 | - |
| end_time | DATETIME | 结束时间 | - |
| status | ENUM | 状态: active/ended/archived | INDEX |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

---

### tutor_messages (辅导消息表)

**用途**: 存储辅导对话的消息记录

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 消息ID (主键) | PRIMARY |
| session_id | VARCHAR(100) | 会话标识 | INDEX |
| user_id | BIGINT | 用户ID (外键) | INDEX |
| role | ENUM | 角色: user/assistant | - |
| content | TEXT | 消息内容 | - |
| message_time | DATETIME | 消息时间 | INDEX |
| create_time | DATETIME | 创建时间 | - |

---

### learning_records (学习记录表)

**用途**: 记录用户的学习行为

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 记录ID (主键) | PRIMARY |
| user_id | BIGINT | 用户ID (外键) | INDEX |
| resource_id | BIGINT | 资源ID (外键) | INDEX |
| action | ENUM | 操作类型: view/start/progress/complete | INDEX |
| duration | INT | 学习时长 (秒) | - |
| progress | INT | 进度 (0-100) | - |
| record_time | DATETIME | 记录时间 | INDEX |
| extra_data | JSON | 额外数据 | - |
| create_time | DATETIME | 创建时间 | - |

---

### learning_reports (学习报告表)

**用途**: 存储学习分析报告

**字段说明**:
| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| id | BIGINT | 报告ID (主键) | PRIMARY |
| user_id | BIGINT | 用户ID (外键) | INDEX |
| report_type | ENUM | 报告类型: daily/weekly/monthly | INDEX |
| start_date | DATE | 开始日期 | INDEX |
| end_date | DATE | 结束日期 | INDEX |
| total_study_time | INT | 总学习时长 (分钟) | - |
| completed_resources | INT | 完成资源数 | - |
| practice_accuracy | DECIMAL(5,2) | 练习正确率 | - |
| learning_progress | DECIMAL(5,2) | 学习进度 | - |
| topic_analysis | JSON | 主题分析 | - |
| suggestions | JSON | 学习建议 (数组) | - |
| trends_data | JSON | 趋势数据 | - |
| create_time | DATETIME | 创建时间 | - |

---

## 数据库使用指南

### 1. 初始化数据库

```bash
# 1. 执行初始化脚本
mysql -u root -p < database/init.sql

# 2. 创建表结构
mysql -u root -p < database/table.sql

# 3. 导入演示数据
mysql -u root -p < database/mock-data.sql
```

### 2. 常用查询示例

**查询学生画像**:
```sql
SELECT u.username, u.nickname, sp.* 
FROM student_profiles sp
JOIN users u ON sp.student_id = u.id
WHERE u.username = 'student001';
```

**获取热门资源**:
```sql
SELECT title, type, rating, views, favorites
FROM resources
WHERE status = 'published'
ORDER BY views DESC, rating DESC
LIMIT 10;
```

**查询学生任务进度**:
```sql
SELECT title, status, progress, start_time, end_time
FROM learning_tasks
WHERE user_id = 4 AND status IN ('todo', 'doing')
ORDER BY priority DESC, end_time ASC;
```

**分析练习正确率**:
```sql
SELECT 
    u.username,
    COUNT(*) as total_questions,
    SUM(CASE WHEN pr.is_correct = 1 THEN 1 ELSE 0 END) as correct_count,
    ROUND(SUM(CASE WHEN pr.is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as accuracy_rate
FROM practice_records pr
JOIN users u ON pr.user_id = u.id
GROUP BY u.username
ORDER BY accuracy_rate DESC;
```

**获取学习时长统计**:
```sql
SELECT 
    u.username,
    COUNT(*) as study_sessions,
    SUM(lr.duration) as total_duration,
    ROUND(AVG(lr.duration), 2) as avg_duration
FROM learning_records lr
JOIN users u ON lr.user_id = u.id
WHERE lr.record_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY u.username
ORDER BY total_duration DESC;
```

### 3. 维护操作

**定期清理过期会话**:
```sql
DELETE FROM tutor_sessions 
WHERE status = 'ended' AND end_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

**更新资源评分**:
```sql
UPDATE resources r
SET rating = (
    SELECT ROUND(AVG(score), 2)
    FROM resource_reviews
    WHERE resource_id = r.id
)
WHERE r.id IN (
    SELECT DISTINCT resource_id FROM resource_reviews
);
```

**归档旧学习报告**:
```sql
UPDATE learning_reports
SET status = 'archived'
WHERE create_time < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

## 性能优化建议

### 1. 索引策略

- **高频查询字段**: 添加普通索引
- **唯一性字段**: 使用 UNIQUE 约束
- **复合查询**: 使用复合索引
- **文本搜索**: 使用全文索引

### 2. 分区策略

对于大数据量表，建议按时间分区:
```sql
-- 学习记录表按月分区
ALTER TABLE learning_records 
PARTITION BY RANGE (TO_DAYS(record_time)) (
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    -- 更多分区...
);
```

### 3. 缓存策略

- **热点数据**: 使用 Redis 缓存
- **用户画像**: 缓存到内存中
- **统计报表**: 定期预计算并缓存

### 4. 数据归档

- **历史数据**: 定期归档到历史表
- **日志数据**: 保留最近3个月，其他归档
- **测试数据**: 定期清理

---

## 安全考虑

### 1. 数据加密

- **密码字段**: 使用 bcrypt 加密
- **敏感信息**: 考虑字段级加密
- **Token存储**: 使用安全的方式存储

### 2. 权限控制

- **数据库用户**: 最小权限原则
- **表级权限**: 按角色分配访问权限
- **行级安全**: 实现数据隔离

### 3. 数据备份

- **定期备份**: 每日增量备份，每周全量备份
- **异地备份**: 重要数据异地存储
- **备份验证**: 定期验证备份完整性

---

## 扩展性设计

### 1. 水平扩展

- **读写分离**: 主库写入，从库读取
- **分库分表**: 按用户ID或时间分片
- **缓存层**: 减轻数据库压力

### 2. 垂直扩展

- **模块化设计**: 不同功能独立存储
- **微服务架构**: 按业务模块拆分
- **数据湖**: 存储分析和历史数据

---

## 常见问题

### Q1: JSON字段如何查询？
```sql
-- 查询包含特定薄弱点的学生
SELECT * FROM student_profiles 
WHERE JSON_CONTAINS(weaknesses, '"递归"');

-- 更新JSON字段
UPDATE student_profiles 
SET weaknesses = JSON_ARRAY_APPEND(weaknesses, '$', '栈')
WHERE student_id = 4;
```

### Q2: 如何处理大量数据插入？
```sql
-- 使用批量插入
INSERT INTO learning_records (user_id, resource_id, action, duration, record_time)
VALUES 
    (1, 1, 'view', 300, NOW()),
    (1, 2, 'view', 450, NOW()),
    (1, 3, 'view', 600, NOW());

-- 使用事务提高性能
START TRANSACTION;
-- 多个插入语句
COMMIT;
```

### Q3: 如何优化慢查询？
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 分析查询执行计划
EXPLAIN SELECT * FROM resources WHERE title LIKE '%递归%';
```

---

*文档版本: v1.0*  
*创建时间: 2026-05-20*  
*维护者: Ocean*  
*数据库版本: MySQL 8.0+*