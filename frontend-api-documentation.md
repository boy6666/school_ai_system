# EduAgent 前端接口文档

## 目录

1. [接口基础规范](#接口基础规范)
2. [认证模块](#认证模块)
3. [用户模块](#用户模块)
4. [学习画像模块](#学习画像模块)
5. [资源模块](#资源模块)
6. [智能辅导模块](#智能辅导模块)
7. [题库练习模块](#题库练习模块)
8. [学习任务模块](#学习任务模块)
9. [学习报告模块](#学习报告模块)
10. [管理后台模块](#管理后台模块)
11. [智能体AI模块](#智能体ai模块)

---

## 接口基础规范

### 1. 请求基础

**基础URL**: `/api`

**请求方法**:
- `GET` - 获取数据
- `POST` - 创建数据
- `PUT` - 更新数据
- `DELETE` - 删除数据

### 2. 请求头

```typescript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {token}"
}
```

### 3. 响应格式

所有接口统一返回格式：

```typescript
{
  "code": 200,        // 状态码：200成功，其他失败
  "message": "成功",   // 提示信息
  "data": {}          // 实际数据
}
```

### 4. 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 参数错误 |
| 401 | 未登录/Token过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 5. 分页参数

```typescript
{
  "page": 1,          // 当前页码
  "pageSize": 10      // 每页数量
}
```

### 6. 分页响应

```typescript
{
  "list": [],         // 数据列表
  "total": 100,       // 总数量
  "page": 1,          // 当前页码
  "pageSize": 10      // 每页数量
}
```

---

## 认证模块

### 1. 用户登录

**接口地址**: `POST /auth/login`

**请求参数**:
```typescript
{
  "username": "student001",    // 用户名
  "password": "123456"        // 密码
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1,
      "username": "student001",
      "nickname": "张三",
      "role": "student",
      "avatar": "/uploads/avatars/student001.jpg"
    }
  }
}
```

### 2. 用户注册

**接口地址**: `POST /auth/register`

**请求参数**:
```typescript
{
  "username": "student002",
  "password": "123456",
  "nickname": "李四",
  "email": "student002@example.com",
  "role": "student"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 2,
    "username": "student002"
  }
}
```

### 3. 用户登出

**接口地址**: `POST /auth/logout`

**请求头**: 需要Authorization

**响应数据**:
```typescript
{
  "code": 200,
  "message": "登出成功"
}
```

### 4. 刷新Token

**接口地址**: `POST /auth/refresh`

**请求头**: 需要Authorization

**响应数据**:
```typescript
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

## 用户模块

### 1. 获取用户信息

**接口地址**: `GET /user/info`

**请求头**: 需要Authorization

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "student001",
    "nickname": "张三",
    "email": "student001@example.com",
    "phone": "13800138000",
    "avatar": "/uploads/avatars/student001.jpg",
    "role": "student",
    "createTime": "2026-05-01T10:00:00",
    "lastLoginTime": "2026-05-19T20:30:00"
  }
}
```

### 2. 更新用户信息

**接口地址**: `PUT /user/info`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "nickname": "张三（更新）",
  "email": "newemail@example.com",
  "phone": "13900139000"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "nickname": "张三（更新）",
    "email": "newemail@example.com"
  }
}
```

### 3. 修改密码

**接口地址**: `PUT /user/password`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "密码修改成功"
}
```

---

## 学习画像模块

### 1. 获取学习画像

**接口地址**: `GET /student/profile`

**请求头**: 需要Authorization

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "studentId": "student_001",
    "major": "计算机相关专业",
    "grade": "大二",
    "course": "Java 数据结构",
    "topic": "递归",
    "learningGoal": "期末考 85 分",
    "knowledgeBase": "有一定编程基础",
    "cognitiveStyle": "偏好图解、代码案例和练习题",
    "weaknesses": ["递归", "二叉树"],
    "mistakePatterns": ["概念混淆", "边界条件遗漏"],
    "resourcePreference": ["讲解文档", "思维导图", "练习题", "代码案例"],
    "pace": "中速",
    "overallType": "稳定提升型",
    "profileSuggestions": [
      "建议围绕薄弱点进行查漏补缺。",
      "建议增加变式练习，提升知识迁移能力。",
      "建议定期复盘错题，形成稳定的解题方法。"
    ],
    "lastUpdated": "2026-05-19T20:30:00"
  }
}
```

### 2. 更新学习画像

**接口地址**: `PUT /student/profile`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "topic": "二叉树",
  "weaknesses": ["递归", "二叉树", "链表"],
  "learningGoal": "期末考 90 分",
  "resourcePreference": ["视频", "代码案例", "练习题"]
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "画像更新成功",
  "data": {
    "studentId": "student_001",
    "topic": "二叉树",
    "weaknesses": ["递归", "二叉树", "链表"],
    "overallType": "稳定提升型",
    "lastUpdated": "2026-05-19T21:00:00"
  }
}
```

### 3. 对话式画像采集

**接口地址**: `POST /student/profile/chat`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "message": "我是计算机大二学生，递归和二叉树不懂，喜欢图解和代码"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "reply": "好的，我了解了。你正在学习数据结构，主要困难是递归和二叉树，更偏好图解和代码案例的学习方式。我会为你提供针对性的学习资源。",
    "profileUpdate": {
      "detectedTopic": "递归",
      "detectedWeaknesses": ["递归", "二叉树"],
      "cognitiveStyle": "偏好图解、代码案例"
    }
  }
}
```

---

## 资源模块

### 1. 获取资源列表

**接口地址**: `GET /resources`

**请求头**: 需要Authorization

**查询参数**:
```typescript
{
  "keyword": "递归",        // 关键词搜索
  "type": "文档",           // 资源类型
  "difficulty": "基础",     // 难度
  "courseId": "java_data_structure",  // 课程ID
  "sort": "hot",           // 排序: hot/new/score
  "page": 1,
  "pageSize": 10
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "递归算法详解",
        "type": "文档",
        "difficulty": "基础",
        "description": "详细讲解递归算法的原理和应用",
        "rating": 4.5,
        "views": 1200,
        "updateTime": "2026-05-15T10:00:00",
        "cover": "/uploads/resources/recursion_cover.jpg",
        "favorite": true,
        "courseId": "java_data_structure",
        "courseName": "Java 数据结构",
        "chapterName": "递归与递归算法",
        "tags": ["递归", "算法", "基础"],
        "fileSize": "2.3MB"
      }
    ],
    "total": 50,
    "recommended": [],
    "hotTags": ["递归", "二叉树", "链表", "排序"]
  }
}
```

### 2. 获取资源详情

**接口地址**: `GET /resources/{id}`

**请求头**: 需要Authorization

**路径参数**:
- `id` - 资源ID

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "递归算法详解",
    "type": "文档",
    "difficulty": "基础",
    "description": "详细讲解递归算法的原理和应用",
    "rating": 4.5,
    "views": 1200,
    "updateTime": "2026-05-15T10:00:00",
    "cover": "/uploads/resources/recursion_cover.jpg",
    "favorite": true,
    "chapterCount": 8,
    "duration": "45分钟",
    "teacher": "张教授",
    "progress": 60,
    "goals": [
      "理解递归的基本概念",
      "掌握递归的终止条件",
      "能够写出简单的递归函数"
    ],
    "suitableFor": ["初学者", "需要复习的学员"],
    "chapters": [
      {
        "id": 1,
        "title": "递归的基本概念",
        "desc": "什么是递归",
        "duration": "5分钟"
      }
    ],
    "reviews": [
      {
        "id": 1,
        "name": "李四",
        "score": 5,
        "content": "讲解很清楚，适合初学者"
      }
    ]
  }
}
```

### 3. 收藏/取消收藏资源

**接口地址**: `POST /resources/{id}/favorite`

**请求头**: 需要Authorization

**路径参数**:
- `id` - 资源ID

**请求参数**:
```typescript
{
  "favorite": true    // true收藏，false取消收藏
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "收藏成功",
  "data": {
    "favorite": true
  }
}
```

### 4. 添加到学习计划

**接口地址**: `POST /resources/{id}/plan`

**请求头**: 需要Authorization

**路径参数**:
- `id` - 资源ID

**响应数据**:
```typescript
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "success": true
  }
}
```

---

## 智能辅导模块

### 1. 获取辅导会话

**接口地址**: `GET /student/tutor/session`

**请求头**: 需要Authorization

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "messages": [
      {
        "id": 1,
        "role": "user",
        "content": "如何理解递归的终止条件？",
        "time": "2026-05-19T20:00:00"
      },
      {
        "id": 2,
        "role": "assistant",
        "content": "递归的终止条件也叫基准情况，它是递归函数停止调用的条件...",
        "time": "2026-05-19T20:00:05"
      }
    ],
    "suggestions": [
      {
        "id": 1,
        "title": "递归基础",
        "prompt": "请详细讲解递归的基础概念"
      },
      {
        "id": 2,
        "title": "递归练习",
        "prompt": "给我一些递归练习题"
      }
    ]
  }
}
```

### 2. 发送辅导消息

**接口地址**: `POST /student/tutor/chat`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "content": "递归和迭代的区别是什么？"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 3,
    "role": "assistant",
    "content": "递归和迭代的主要区别在于实现方式不同...",
    "time": "2026-05-19T20:01:00"
  }
}
```

---

## 题库练习模块

### 1. 获取练习题列表

**接口地址**: `GET /practice/list`

**请求头**: 需要Authorization

**查询参数**:
```typescript
{
  "topic": "递归",        // 主题
  "difficulty": "基础",    // 难度
  "type": "选择题",       // 题型
  "page": 1,
  "pageSize": 10
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "type": "选择题",
        "difficulty": "基础",
        "question": "递归函数必须包含哪两个要素？",
        "options": [
          "参数和返回值",
          "基准情况（终止条件）和递归调用",
          "变量和常量",
          "循环和条件"
        ],
        "answer": "基准情况（终止条件）和递归调用"
      }
    ],
    "total": 20
  }
}
```

### 2. 获取练习题详情

**接口地址**: `GET /practice/{id}`

**请求头**: 需要Authorization

**路径参数**:
- `id` - 题目ID

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "type": "选择题",
    "difficulty": "基础",
    "question": "递归函数必须包含哪两个要素？",
    "options": [
      "参数和返回值",
      "基准情况（终止条件）和递归调用",
      "变量和常量",
      "循环和条件"
    ],
    "answer": "基准情况（终止条件）和递归调用",
    "analysis": "递归函数必须明确终止条件（防止无限递归）和递归调用（将问题分解为更小的子问题）。",
    "tags": ["递归", "算法", "基础"]
  }
}
```

### 3. 提交答案

**接口地址**: `POST /practice/submit`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "questionId": 1,
  "answer": "基准情况（终止条件）和递归调用"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "提交成功",
  "data": {
    "correct": true,
    "yourAnswer": "基准情况（终止条件）和递归调用",
    "correctAnswer": "基准情况（终止条件）和递归调用",
    "analysis": "递归函数必须明确终止条件（防止无限递归）和递归调用（将问题分解为更小的子问题）。",
    "score": 10
  }
}
```

### 4. 获取练习进度

**接口地址**: `GET /practice/progress`

**请求头**: 需要Authorization

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "totalQuestions": 100,
    "completedQuestions": 45,
    "correctQuestions": 38,
    "accuracyRate": 84.4,
    "totalScore": 380,
    "topicProgress": [
      {
        "topic": "递归",
        "total": 20,
        "completed": 15,
        "correct": 13,
        "accuracy": 86.7
      }
    ]
  }
}
```

---

## 学习任务模块

### 1. 获取学习任务

**接口地址**: `GET /student/tasks`

**请求头**: 需要Authorization

**查询参数**:
```typescript
{
  "keyword": "递归",     // 关键词
  "status": "todo",      // 状态: todo/doing/done
  "priority": "high"     // 优先级: high/middle/low
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "summary": {
      "todayCount": 5,
      "weekCount": 15,
      "doneCount": 45,
      "averageProgress": 75
    },
    "list": [
      {
        "id": 1,
        "title": "完成递归算法学习",
        "courseName": "Java 数据结构",
        "chapterName": "递归与递归算法",
        "startTime": "2026-05-19T09:00:00",
        "endTime": "2026-05-19T11:00:00",
        "priority": "high",
        "status": "doing",
        "progress": 60
      }
    ]
  }
}
```

### 2. 更新任务状态

**接口地址**: `POST /student/tasks/{id}/status`

**请求头**: 需要Authorization

**路径参数**:
- `id` - 任务ID

**请求参数**:
```typescript
{
  "status": "done"    // todo/doing/done
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "success": true
  }
}
```

---

## 学习报告模块

### 1. 获取学习报告

**接口地址**: `GET /student/report`

**请求头**: 需要Authorization

**查询参数**:
```typescript
{
  "startDate": "2026-05-01",
  "endDate": "2026-05-19",
  "type": "weekly"    // daily/weekly/monthly
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "summary": {
      "totalStudyTime": 1200,
      "completedResources": 15,
      "practiceAccuracy": 85.5,
      "learningProgress": 75
    },
    "topicAnalysis": [
      {
        "topic": "递归",
        "studyTime": 300,
        "accuracy": 90,
        "progress": 80
      }
    ],
    "suggestions": [
      "继续加强递归和二叉树的练习",
      "建议增加链表相关学习时间",
      "保持当前的学习节奏"
    ],
    "trends": {
      "daily": [
        {
          "date": "2026-05-01",
          "studyTime": 60,
          "accuracy": 80
        }
      ]
    }
  }
}
```

---

## 管理后台模块

### 1. 获取用户列表

**接口地址**: `GET /admin/users`

**请求头**: 需要Authorization (管理员权限)

**查询参数**:
```typescript
{
  "keyword": "张",
  "role": "student",
  "status": "active",
  "page": 1,
  "pageSize": 10
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "username": "student001",
        "nickname": "张三",
        "email": "student001@example.com",
        "role": "student",
        "status": "active",
        "createTime": "2026-05-01T10:00:00",
        "lastLoginTime": "2026-05-19T20:30:00"
      }
    ],
    "total": 100
  }
}
```

### 2. 创建用户

**接口地址**: `POST /admin/users`

**请求头**: 需要Authorization (管理员权限)

**请求参数**:
```typescript
{
  "username": "teacher001",
  "password": "123456",
  "nickname": "王老师",
  "email": "teacher001@example.com",
  "role": "teacher"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "userId": 10,
    "username": "teacher001"
  }
}
```

### 3. 获取资源管理列表

**接口地址**: `GET /admin/resources`

**请求头**: 需要Authorization (管理员权限)

**查询参数**:
```typescript
{
  "keyword": "递归",
  "type": "文档",
  "status": "published",
  "page": 1,
  "pageSize": 10
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "递归算法详解",
        "type": "文档",
        "author": "张教授",
        "status": "published",
        "createTime": "2026-05-15T10:00:00",
        "views": 1200
      }
    ],
    "total": 50
  }
}
```

---

## 智能体AI模块

### 1. 智能体对话

**接口地址**: `POST /ai/chat`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "studentId": "student_001",
  "sessionId": "session_001",
  "userInput": "我是计算机大二学生，递归和二叉树不懂，喜欢图解和代码"
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "intent": "explain",
    "intentConfidence": 0.85,
    "routeReason": "用户表达了对递归和二叉树的理解困难，适合使用讲解智能体",
    "finalAnswer": "好的，我来帮你讲解递归和二叉树的相关内容...",
    "profile": {
      "studentId": "student_001",
      "topic": "递归",
      "weaknesses": ["递归", "二叉树"],
      "overallType": "稳定提升型"
    },
    "resources": [],
    "learningPath": [],
    "safetyReport": {
      "passed": true,
      "riskLevel": "low"
    },
    "evaluationReport": {},
    "resourceDir": ""
  }
}
```

### 2. 个性化资源生成

**接口地址**: `POST /ai/resources/generate`

**请求头**: 需要Authorization

**请求参数**:
```typescript
{
  "studentId": "student_001",
  "topic": "递归",
  "resourceTypes": ["文档", "视频", "练习题"]
}
```

**响应数据**:
```typescript
{
  "code": 200,
  "message": "成功",
  "data": {
    "resources": [
      {
        "type": "文档",
        "title": "递归算法详解",
        "content": "...",
        "difficulty": "基础"
      }
    ],
    "learningPath": [
      {
        "step": 1,
        "title": "学习递归基本概念",
        "resources": ["recursion_basic.pdf"]
      }
    ],
    "resourceDir": "/data/resources/student_001/session_001"
  }
}
```

### 3. 健康检查

**接口地址**: `GET /ai/health`

**响应数据**:
```typescript
{
  "code": 200,
  "message": "服务正常",
  "data": {
    "status": "ok",
    "version": "0.1.0",
    "timestamp": "2026-05-19T20:30:00"
  }
}
```

---

## 接口使用示例

### 完整的API调用流程

```typescript
import { login, getStudentTasks, sendTutorMessage } from '@/api'
import request from '@/utils/request'

// 1. 用户登录
const handleLogin = async () => {
  try {
    const response = await login({
      username: 'student001',
      password: '123456'
    })

    // 保存Token
    localStorage.setItem('token', response.token)

    // 保存用户信息
    localStorage.setItem('userInfo', JSON.stringify(response.userInfo))

    return response
  } catch (error) {
    console.error('登录失败:', error)
    throw error
  }
}

// 2. 获取学习任务
const fetchLearningTasks = async () => {
  try {
    const response = await getStudentTasks({
      status: 'todo',
      priority: 'high'
    })

    console.log('学习任务:', response.list)
    return response
  } catch (error) {
    console.error('获取任务失败:', error)
    throw error
  }
}

// 3. 发送辅导消息
const sendMessage = async (content: string) => {
  try {
    const response = await sendTutorMessage(content)
    console.log('AI回复:', response.content)
    return response
  } catch (error) {
    console.error('发送消息失败:', error)
    throw error
  }
}

// 完整使用流程
const main = async () => {
  // 登录
  await handleLogin()

  // 获取任务
  await fetchLearningTasks()

  // 发送问题
  await sendMessage('如何理解递归的终止条件？')
}
```

---

## 错误处理

### 统一错误处理

```typescript
import request from '@/utils/request'

// 添加响应拦截器
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data

    if (code === 200) {
      return data
    } else if (code === 401) {
      // Token过期，跳转登录页
      localStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject(new Error('登录已过期，请重新登录'))
    } else {
      // 显示错误提示
      console.error('接口错误:', message)
      return Promise.reject(new Error(message))
    }
  },
  error => {
    console.error('网络错误:', error)

    // 处理网络错误
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('请求超时，请检查网络连接'))
    } else if (error.response) {
      return Promise.reject(new Error('服务器错误，请稍后重试'))
    } else {
      return Promise.reject(new Error('网络连接失败'))
    }
  }
)
```

---

## 开发环境配置

### 环境变量

**开发环境** (`.env.development`):
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

**生产环境** (`.env.production`):
```env
VITE_API_BASE_URL=/api
```

### Vite代理配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

---

## 注意事项

1. **Token管理**: 所有需要认证的接口都必须在请求头中携带Token
2. **错误处理**: 必须统一处理接口错误，提供友好的错误提示
3. **Loading状态**: 请求过程中显示加载状态，提升用户体验
4. **数据缓存**: 合理使用缓存，减少不必要的请求
5. **请求节流**: 防止频繁请求，对高频操作进行节流处理
6. **接口封装**: 所有接口必须通过 `src/api/` 目录下的文件调用
7. **类型定义**: 使用TypeScript定义接口类型，提高代码质量

---

*文档版本: v1.0*  
*更新时间: 2026-05-19*  
*维护者: Ocean*