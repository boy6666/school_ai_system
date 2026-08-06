# EduAgent Git 协作守则

> 适用范围：全体后端 / 前端 / AI 开发同学。违反以下规则导致的冲突、丢代码，由当事人负责。
> 配套文件：`.github/CODEOWNERS`（模块责任人 + 自动派审）、`docs/superpowers/task-schedule.md`（任务排期与 DoD）。

---

## 1. 分支模型（四线 + 个人模块分支）

| 分支 | 角色 | 能否直推 |
|------|------|----------|
| `main` | 稳定 / 可演示基线 | ❌ 仅 PR + 审批 + CI 绿 |
| `develop` | 单体基线（比赛交付物） | ❌ 保留，不动 |
| `feature/microservice` | 微服务后端集成线 | ❌ 仅 PR 合入 |
| `feat/<module>` | **个人模块分支**（common/gateway/auth/code/ai/resource/learning/teacher/web） | ✅ 个人自由推送，但**合入需 PR** |

- 所有 `feat/<module>` 分支统一 **PR 回 `feature/microservice`**。
- 个人分支从 `feature/microservice` 起（`feat/web` 含 `edu-agent-web`，`feat/ai` 含 `edu-agent-ai`）。
- **禁止**在 `main` / `develop` / `feature/microservice` 上直接提交。

### 模块 → 负责人 → 分支

| 模块 | 负责人 | 分支 | GitHub |
|------|--------|------|--------|
| common / gateway / auth / code / teacher | 吴友诚(WYC) | `feat/common` `feat/gateway` `feat/auth` `feat/code` `feat/teacher` | @boy6666 |
| resource（含 KB 清洗） | 陈嘉成(B) | `feat/resource` | @Reverie999 |
| learning / ai | 陈海洋(C) | `feat/learning` `feat/ai` | @baixiong157-creator |
| web（Vue3 三端，8.15 后） | 曾姿妍(zzy/D) | `feat/web` | @bianjiabobohu916-cloud |

---

## 2. 每日标准流程（每人照做）

```bash
# 1. 拉最新集成线
git fetch origin

# 2. 首次建/切换自己的模块分支
git switch -c feat/resource origin/feat/resource     # 首次
git switch feat/resource                              # 之后

# 3. 开发：只改自己模块目录（edu-agent-<module>/ 或 edu-agent-web/）
#    ❌ 不要碰 common/ gateway/ 其他人的模块

# 4. 提交（见 §4 规范）
git add edu-agent-resource/...
git commit -m "feat(resource): KB 采集清洗流水线"

# 5. 推送个人分支
git push -u origin feat/resource

# 6. GitHub 开 PR：feat/resource → feature/microservice
#    等 CODEOWNERS 自动派的 reviewer 审批 + CI 绿后由架构合入
```

### 开 PR 前必做
```bash
git fetch origin
git rebase origin/feature/microservice     # 把个人分支变基到最新集成线，避免冲突堆积
```
变基后若远端个人分支已推过，需 `git push --force-with-lease`（仅个人分支允许）。

---

## 3. 协作红线（违反即追责）

1. **禁止直推集成线**：`feature/microservice` / `main` / `develop` 一律 PR。
2. **禁止跨模块改代码**：common、gateway 非架构批准不得改；不要改他人的 `feat/<module>` 目录。
3. **DB-per-service**：禁止跨服务直连对方数据库；共享数据走 Feign 调用或 MQ 事件。
4. **开 PR 前先 rebase** 到 `feature/microservice` 最新，保持分支新鲜。
5. **CI 必须绿**（`mvn clean verify`）才能合入；本地编译不过不要推。
6. **大文件不入库**：zip / 视频 / 学习类 PDF 已被 `.gitignore` 忽略，勿 `git add -f` 强行加。

---

## 4. 提交信息规范

格式：`type(scope): 简述`（英文 scope 用模块名）

| type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修 bug |
| `style` | 仅样式/格式（zzy 美化用 `style(web)`） |
| `refactor` | 重构 |
| `docs` | 文档 |
| `chore` | 构建/配置 |

示例：
- `feat(auth): 登录注册 + JWT 签发`
- `feat(resource): KB 采集清洗流水线`
- `fix(learning): 修复路径聚合统计偏差`
- `style(web): 学生端课程中心视觉美化`
- `chore: 补充 Nacos 配置`

关联任务：PR 描述里写 `closes #<Issue号>`（Issue 号见 `task-schedule.md` 行号映射）。

---

## 5. 常用救命命令

```bash
git status                       # 先看当前状态，再决定
git stash                        # 临时保存未提交改动
git switch -c feat/xxx origin/feat/xxx   # 基于远端建个人分支
git rebase origin/feature/microservice   # 同步集成线
git push --force-with-lease      # 仅个人分支 rebase 后强推
git log --oneline -10            # 看提交历史
git diff origin/feature/microservice  # 看与集成线差异
```
