# EduAgent 智能教育系统 - Docker 部署指南

## 📦 交付方式

本系统采用 **Docker Compose** 进行容器化部署，可一键启动全部服务。

## 🖥️ 系统要求

| 组件 | 最低要求 |
|------|----------|
| Docker | 20.10+ |
| Docker Compose | 2.0+ |
| 内存 | 4GB+ |
| 磁盘 | 5GB+ |

**Windows 用户**: 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

**macOS 用户**: 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

**Linux 用户**: 安装 Docker Engine 和 Docker Compose Plugin

## 🚀 快速启动

### 方式一：一键启动（推荐）

```bash
# Windows 用户双击运行
start-docker.bat

# 或在终端中运行
docker-compose up -d
```

### 方式二：分步启动

```bash
# 1. 构建镜像
docker-compose build

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f
```

## 🌐 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost |
| 后端 API | http://localhost:8080 |
| AI 服务 | http://localhost:8000 |

## 👤 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 学生 | teststudent | student123 |

## 🔧 常用命令

```bash
# 停止所有服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f [服务名]

# 重新构建
docker-compose up -d --build

# 进入容器
docker exec -it edu-server bash
docker exec -it edu-ai bash
```

## 📁 项目结构

```
edu-agent/
├── docker-compose.yml     # 容器编排配置
├── Dockerfile.ai          # AI服务镜像
├── Dockerfile.server      # 后端服务镜像
├── Dockerfile.web         # 前端镜像
├── nginx/nginx.conf       # Nginx配置
├── database/init.sql      # 数据库初始化脚本
├── edu-agent-ai/          # AI服务（Python）
├── edu-agent-server/      # 后端服务（Java）
├── edu-agent-web/         # 前端（Vue）
└── start-docker.bat       # Windows一键启动脚本
```

## 🔍 故障排查

### MySQL 连接失败
```bash
# 检查MySQL容器状态
docker-compose logs mysql

# 等待MySQL完全启动后重试
docker-compose restart edu-server
```

### 端口冲突
如果 80/3306/8080/8000 端口被占用，修改 `docker-compose.yml` 中的端口映射。

### 前端无法访问后端
```bash
# 检查nginx代理配置
docker-compose logs edu-web
```

## 🗑️ 清理

```bash
# 停止并删除容器
docker-compose down

# 删除镜像（可选）
docker-compose down --rmi all

# 删除数据卷（重置数据库）
docker-compose down -v
```

---

**比赛交付版本**: v4.1.6