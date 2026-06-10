# EduAgent 前端开发工作手册

## 1. 手册目的

本文档用于规范 EduAgent 项目前端开发流程，统一开发环境、目录结构、Git 协作方式、页面开发流程、接口联调方式和部署规则。

项目采用前后端分离架构：

```text
前端：Vue 3 + Vite + TypeScript
后端：Spring Boot
接口管理：Apifox
版本管理：Git
部署展示：Nginx
```

---

## 2. 开发阶段结论

开发阶段不使用 Nginx 作为主要访问入口。

本地开发使用 Vite：

```bash
npm run dev
```

正式展示或部署测试时，再使用 Nginx 托管前端打包文件。

整体流程：

```text
本地开发：Vite
接口联调：Apifox Mock / Spring Boot 本地接口
部署展示：Nginx + Spring Boot
代码协作：Git 分支开发
```

---

## 3. 开发环境规范

### 3.1 推荐软件

| 软件 | 用途 |
|---|---|
| VS Code | 前端开发编辑器 |
| Git for Windows | Git 命令与 Git Bash |
| Node.js LTS | 前端运行环境 |
| npm | 前端包管理 |
| Apifox | 接口文档、Mock、联调 |
| Chrome / Edge | 页面调试 |
| Nginx | 部署展示阶段使用 |

### 3.2 终端规范

统一使用以下终端之一：

```text
Git Bash
VS Code 内置 Git Bash
```

MSYS2 UCRT64 可以保留，但不作为 Vue 前端项目的主开发终端。

原因：

```text
避免 Node 路径不一致
避免 npm 环境不一致
避免脚本执行差异
避免换行符和路径问题
```

---

## 4. 项目目录结构

```text
edu-agent/
├── edu-agent-web/              # 前端 Vue 项目
│   ├── src/
│   │   ├── api/                # 接口请求封装
│   │   ├── assets/             # 图片、图标、静态资源
│   │   ├── components/         # 公共组件
│   │   ├── layouts/            # 学生端/管理端布局
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── views/              # 页面
│   │   │   ├── student/        # 学生端页面
│   │   │   └── admin/          # 管理端页面
│   │   └── utils/              # 工具函数
│   ├── public/                 # 公共静态资源
│   ├── package.json
│   └── vite.config.ts
│
├── edu-agent-server/           # Spring Boot 后端
│
├── docs/                       # 项目文档
├── database/                   # 数据库脚本
├── nginx/                      # Nginx 配置
├── apifox/                     # Apifox 导出文档
├── scripts/                    # 构建、启动、部署脚本
├── .gitignore
├── .editorconfig
├── .gitattributes
└── README.md
```

---

## 5. 目录说明

### 5.1 `edu-agent-web/`

前端主项目目录，负责页面展示、路由跳转、状态管理、接口请求和用户交互。

### 5.2 `edu-agent-server/`

后端项目目录，负责登录认证、业务接口、数据存储、智能体调用和权限控制。

### 5.3 `docs/`

项目文档目录。

建议包含：

```text
docs/
├── requirement.md       # 需求分析
├── architecture.md      # 系统架构
├── frontend-manual.md   # 前端开发手册
├── api/                 # 接口文档
├── database/            # 数据库设计
├── deploy/              # 部署说明
└── images/              # 原型图、架构图、流程图
```

### 5.4 `database/`

数据库脚本目录。

```text
database/
├── init.sql             # 创建数据库
├── table.sql            # 建表语句
├── mock-data.sql        # 演示数据
└── update/              # 后续数据库变更脚本
```

数据库结构发生变化时，必须提交对应 SQL 文件。

### 5.5 `nginx/`

Nginx 配置目录。

```text
nginx/
├── edu-agent.conf
└── README.md
```

用于保存前端部署和接口代理配置。

### 5.6 `apifox/`

接口文档导出目录。

```text
apifox/
├── edu-agent-api.json
└── README.md
```

接口发生变更后，需要同步更新 Apifox，并提交导出的接口文档。

### 5.7 `scripts/`

常用脚本目录。

```text
scripts/
├── build-web.sh
├── start-backend.sh
├── stop-backend.sh
└── deploy.sh
```

---

## 6. 前端开发顺序

页面可以一个一个开发，但必须先完成基础骨架。

推荐顺序：

```text
1. 初始化 Vue 项目
2. 安装 UI 组件库
3. 配置路由
4. 配置 Pinia
5. 封装 Axios
6. 搭建学生端 Layout
7. 搭建管理端 Layout
8. 开发登录页
9. 开发学生端页面
10. 开发管理端页面
11. 接入 Apifox Mock
12. 接入真实后端接口
13. 打包测试
14. Nginx 部署
```

---

## 7. 页面开发范围

### 7.1 学生端页面

```text
/student/dashboard              学生首页
/student/profile/chat           对话式学习画像
/student/profile/overview       学习画像概览
/student/resources/generate     多智能体资源生成
/student/resources              资源中心
/student/path                   个性化学习路径
/student/tutor                  智能辅导
/student/report                 学习报告
/student/profile                个人中心
```

### 7.2 管理端页面

```text
/admin/dashboard                管理后台首页
/admin/users                    用户管理
/admin/resources                资源管理
/admin/agents                   智能体管理
/admin/reviews                  内容审核
/admin/statistics               数据统计
/admin/settings                 系统设置
```

---

## 8. 前端文件命名规范

### 8.1 页面文件

学生端：

```text
src/views/student/Dashboard.vue
src/views/student/ProfileChat.vue
src/views/student/ProfileOverview.vue
src/views/student/ResourceGenerate.vue
src/views/student/ResourceCenter.vue
src/views/student/LearningPath.vue
src/views/student/TutorChat.vue
src/views/student/LearningReport.vue
```

管理端：

```text
src/views/admin/Dashboard.vue
src/views/admin/UserManage.vue
src/views/admin/ResourceManage.vue
src/views/admin/AgentManage.vue
src/views/admin/ContentReview.vue
src/views/admin/Statistics.vue
src/views/admin/SystemSetting.vue
```

### 8.2 公共组件

公共组件放在：

```text
src/components/
```

推荐组件：

```text
PageHeader.vue
AiCard.vue
ResourceCard.vue
AgentFlow.vue
ProfileRadar.vue
LearningProgress.vue
MarkdownViewer.vue
EmptyState.vue
```

### 8.3 API 文件

接口请求统一放在：

```text
src/api/
```

推荐拆分：

```text
src/api/auth.ts
src/api/profile.ts
src/api/resource.ts
src/api/path.ts
src/api/tutor.ts
src/api/report.ts
src/api/admin.ts
```

页面中不直接写接口地址，必须通过 `src/api/` 调用。

---

## 9. 前端本地开发命令

进入前端目录：

```bash
cd edu-agent-web
```

安装依赖：

```bash
npm install
```

启动开发环境：

```bash
npm run dev
```

打包：

```bash
npm run build
```

预览打包结果：

```bash
npm run preview
```

---

## 10. 接口请求规范

### 10.1 环境变量

`edu-agent-web/.env.development`：

```env
VITE_API_BASE_URL=/api
```

`edu-agent-web/.env.production`：

```env
VITE_API_BASE_URL=/api
```

### 10.2 Axios 封装

`src/utils/request.ts`：

```ts
import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default request
```

### 10.3 Vite 开发代理

`vite.config.ts`：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
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

开发阶段访问：

```text
http://localhost:5173
```

前端请求：

```text
/api/auth/login
```

会被代理到：

```text
http://localhost:8080/api/auth/login
```

---

## 11. Apifox 使用规范

### 11.1 Apifox 分组

建议接口分组：

```text
认证模块
学习画像模块
资源生成模块
学习路径模块
智能辅导模块
题库练习模块
学习报告模块
管理后台模块
文件上传模块
```

### 11.2 Apifox 环境

```text
Mock 环境：Apifox Mock 地址
本地后端：http://localhost:8080
测试环境：http://服务器IP/api
```

### 11.3 联调流程

```text
1. Apifox 先定义接口
2. 前端根据 Mock 数据开发页面
3. 后端按照接口文档开发 API
4. Apifox 测试后端接口
5. 前端切换真实接口
6. 修复字段、参数、状态码问题
7. 完成页面联调
```

### 11.4 接口变更规则

接口变更必须记录：

```text
接口：POST /api/resources/generate
变更：新增 resourceTypes 字段
影响页面：资源生成页
影响模块：resource.ts
```

接口变更后需要：

```text
1. 更新 Apifox
2. 导出接口文档
3. 放入 apifox/ 或 docs/api/
4. 提交 Git
```

提交示例：

```bash
git add apifox docs/api
git commit -m "docs: 更新资源生成接口文档"
```

---

## 12. Git 分支规范

### 12.1 分支类型

```text
main        稳定演示版本
develop     日常开发主分支
feature/*   功能开发分支
fix/*       问题修复分支
```

### 12.2 推荐分支

```text
main
develop
feature/frontend-layout
feature/frontend-login
feature/frontend-dashboard
feature/frontend-profile
feature/frontend-resource
feature/frontend-path
feature/frontend-tutor
feature/frontend-report
feature/frontend-admin
fix/frontend-router
fix/frontend-style
```

### 12.3 分支规则

```text
1. main 分支只放稳定版本
2. 日常开发基于 develop 分支
3. 每个功能单独创建 feature 分支
4. 修复问题创建 fix 分支
5. 不直接向 main 提交代码
6. 功能完成后合并到 develop
7. 演示版本稳定后再合并到 main
```

---

## 13. Git 初始化流程

组长初始化项目：

```bash
mkdir edu-agent
cd edu-agent
git init
```

创建目录：

```bash
mkdir edu-agent-web
mkdir edu-agent-server
mkdir docs
mkdir database
mkdir nginx
mkdir apifox
mkdir scripts
```

创建基础文件：

```bash
touch README.md
touch .gitignore
touch .editorconfig
touch .gitattributes
```

首次提交：

```bash
git add .
git commit -m "chore: 初始化项目目录结构"
```

关联远程仓库：

```bash
git remote add origin https://gitee.com/团队空间/edu-agent.git
git branch -M main
git push -u origin main
```

创建开发分支：

```bash
git checkout -b develop
git push -u origin develop
```

---

## 14. 成员首次拉取项目

```bash
git clone https://gitee.com/团队空间/edu-agent.git
cd edu-agent
```

切换开发分支：

```bash
git checkout develop
git pull origin develop
```

---

## 15. 日常开发流程

### 15.1 开发前

```bash
git checkout develop
git pull origin develop
```

创建功能分支：

```bash
git checkout -b feature/frontend-profile
```

如果分支已存在：

```bash
git checkout feature/frontend-profile
git merge develop
```

### 15.2 开发中

启动前端：

```bash
cd edu-agent-web
npm run dev
```

完成一个页面或一个功能后，先自测。

### 15.3 提交代码

查看修改：

```bash
git status
```

添加文件：

```bash
git add .
```

提交：

```bash
git commit -m "feat: 新增学习画像页面"
```

推送：

```bash
git push -u origin feature/frontend-profile
```

后续推送：

```bash
git push
```

### 15.4 合并代码

在 Gitee / GitHub 创建 Pull Request：

```text
feature/frontend-profile → develop
```

合并前检查：

```text
页面是否能打开
路由是否正确
控制台是否有红色报错
接口是否封装在 src/api
是否提交了无关文件
是否影响其他页面
```

---

## 16. Commit 提交规范

提交格式：

```text
类型: 描述
```

常用类型：

| 类型 | 含义 |
|---|---|
| feat | 新功能 |
| fix | 修复问题 |
| docs | 文档修改 |
| style | 样式调整 |
| refactor | 代码重构 |
| chore | 配置、构建、依赖修改 |
| test | 测试相关 |

示例：

```bash
git commit -m "feat: 新增学生端首页"
git commit -m "feat: 新增资源中心页面"
git commit -m "fix: 修复侧边栏选中状态错误"
git commit -m "style: 优化学习路径页面布局"
git commit -m "docs: 更新前端开发手册"
git commit -m "chore: 新增 Nginx 配置文件"
```

禁止使用：

```text
修改
更新
123
最终版
又改了一点
```

---

## 17. Git 冲突处理

拉取或合并时出现冲突：

```bash
git merge develop
```

冲突文件中会出现：

```text
<<<<<<< HEAD
当前分支代码
=======
合并进来的代码
>>>>>>> develop
```

处理步骤：

```text
1. 打开冲突文件
2. 删除冲突标记
3. 保留最终正确代码
4. 本地运行测试
5. 提交冲突修复
```

命令：

```bash
git add .
git commit -m "fix: 解决前端路由合并冲突"
git push
```

---

## 18. 容易冲突的文件

以下文件多人同时修改时容易冲突：

```text
src/router/index.ts
src/layouts/
src/stores/
src/api/
package.json
```

处理规则：

```text
router/index.ts 由固定成员维护
package.json 新增依赖前先记录
Layout 文件避免多人同时大改
API 按模块拆分，减少多人修改同一文件
```

---

## 19. 依赖管理规范

统一使用 npm。

安装依赖：

```bash
npm install
```

新增依赖：

```bash
npm install 依赖名
```

新增依赖后必须提交：

```text
package.json
package-lock.json
```

禁止混用：

```text
npm
pnpm
yarn
```

---

## 20. `.gitignore` 规范

根目录 `.gitignore`：

```gitignore
# Node
node_modules/
dist/
npm-debug.log*
pnpm-debug.log*

# Java
target/
*.class
*.jar
*.war

# IDE
.idea/
.vscode/
*.iml

# Logs
logs/
*.log

# Env
.env
.env.local
application-prod.yml

# Uploads
uploads/

# OS
.DS_Store
Thumbs.db
```

禁止提交：

```text
node_modules/
dist/
target/
.env
.env.local
application-prod.yml
日志文件
上传文件
数据库密码
AI API Key
JWT 密钥
```

---

## 21. `.editorconfig` 规范

根目录 `.editorconfig`：

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 2

[*.java]
indent_size = 4

[*.md]
trim_trailing_whitespace = false
```

---

## 22. `.gitattributes` 规范

根目录 `.gitattributes`：

```gitattributes
* text=auto eol=lf

*.bat text eol=crlf
*.cmd text eol=crlf
*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.ico binary
```

---

## 23. 页面开发流程

每个页面按照以下流程开发：

```text
1. 确认页面原型
2. 新建 Vue 页面文件
3. 配置路由
4. 添加菜单入口
5. 使用静态假数据完成页面
6. 抽取可复用组件
7. 接入 Apifox Mock
8. 接入真实后端接口
9. 本地自测
10. 提交 Git
```

---

## 24. 页面完成标准

页面合并前必须满足：

```text
页面能正常打开
路由地址正确
刷新页面不报错
没有明显样式错位
接口请求封装在 src/api
没有控制台红色报错
按钮、弹窗、表格、分页基本可用
有 loading 状态
有空状态
有错误提示
```

---

## 25. Nginx 使用阶段

### 25.1 开发阶段

不使用 Nginx。

使用：

```bash
npm run dev
```

访问：

```text
http://localhost:5173
```

### 25.2 联调稳定阶段

开始测试打包结果和 Nginx 配置。

```bash
npm run build
```

检查 `dist/` 是否生成。

### 25.3 演示部署阶段

使用 Nginx 托管前端页面，并代理后端接口。

```text
Nginx：80 端口
Spring Boot：8080 端口
MySQL：3306 端口
Redis：6379 端口
```

---

## 26. Nginx 配置示例

`nginx/edu-agent.conf`：

```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html/edu-agent;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

注意：

```text
Vue Router 使用 history 模式时，必须配置 try_files。
否则刷新页面可能出现 404。
```

---

## 27. 开发与部署区别

| 阶段 | 访问方式 | 是否使用 Nginx | 是否支持热更新 |
|---|---|---|---|
| 本地开发 | http://localhost:5173 | 否 | 是 |
| 本地联调 | http://localhost:5173 + /api | 否 | 是 |
| 测试部署 | http://服务器IP | 是 | 否 |
| 比赛演示 | http://服务器IP | 是 | 否 |

---

## 28. 前端任务拆分建议

### A 组：基础框架

```text
项目初始化
路由配置
Axios 封装
Pinia 配置
学生端 Layout
管理端 Layout
登录页
```

### B 组：学生端核心页面

```text
学生首页
对话式学习画像
学习画像概览
资源生成
资源中心
学习路径
```

### C 组：辅导、报告与管理端

```text
智能辅导
学习报告
管理后台首页
用户管理
资源管理
智能体管理
内容审核
系统设置
```

---

## 29. 推荐开发里程碑

### 第一阶段：基础框架

```text
Vue 项目初始化
路由可用
Layout 可用
登录页可用
Axios 封装完成
```

### 第二阶段：学生端页面

```text
学生首页完成
学习画像页面完成
资源生成页面完成
学习路径页面完成
智能辅导页面完成
学习报告页面完成
```

### 第三阶段：管理端页面

```text
管理后台首页完成
用户管理完成
资源管理完成
智能体管理完成
内容审核完成
系统设置完成
```

### 第四阶段：接口联调

```text
Apifox Mock 接入
真实后端接口接入
登录状态联调
页面数据联调
错误状态处理
```

### 第五阶段：部署演示

```text
npm run build
Nginx 部署 dist
/api 代理 Spring Boot
演示数据准备
完整流程测试
```

---

## 30. 最终工作流

```text
拉取 develop 最新代码
    ↓
创建 feature 分支
    ↓
开发页面或功能
    ↓
本地自测
    ↓
提交 commit
    ↓
推送远程分支
    ↓
创建 Pull Request
    ↓
合并到 develop
    ↓
阶段测试
    ↓
稳定后合并 main
```

---

## 31. 核心规则

```text
1. 开发阶段使用 Vite，不使用 Nginx。
2. Nginx 用于部署测试和比赛演示。
3. 页面可以逐个开发，但必须先完成 Layout、路由、Axios。
4. 所有接口请求统一放在 src/api/。
5. 所有功能开发必须从 develop 创建 feature 分支。
6. 不直接向 main 提交代码。
7. 提交信息必须清楚表达修改内容。
8. 不提交 node_modules、dist、target、日志、密钥。
9. 接口变更必须同步 Apifox。
10. 页面合并前必须完成本地自测。
```

