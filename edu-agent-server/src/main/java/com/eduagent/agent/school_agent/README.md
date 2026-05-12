# 个性化学习智能体 – School AI System

基于 LangGraph 和科大讯飞大模型的智能学习辅助系统，自动分析学生画像、检索知识库、生成五类学习资源并规划学习路径。

## 功能特性

- **智能画像构建**：从自然语言中抽取 8 个维度学生画像
- **知识库检索**：简单关键词匹配（可扩展为向量检索）
- **资源生成**：包含课程讲解、思维导图、练习题、拓展阅读、代码实践
- **学习路径规划**：5 步个性化学习路径
- **文件持久化**：自动保存 JSON / Mermaid / Python 文件
- **Guardrails 校验**：严格校验数据结构，保障输出质量

## 环境要求

- Python 3.10+
- 科大讯飞星火 API 免费账号（或其他 OpenAI 兼容接口）

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置 API 凭证（以科大讯飞为例）

1. 访问 [讯飞开放平台](https://www.xfyun.cn/) 注册/登录。
2. 创建应用并订阅 **Spark Lite** 服务（永久免费）。
3. 记录 `APIKey` 和 `APISecret`。

编辑 `school_agent/config.py` 或创建 `.env` 文件：

python

```
MODEL_NAME = "lite"
BASE_URL = "https://spark-api-open.xf-yun.com/v1"
API_KEY = "你的APIKey:你的APISecret"   # 注意用英文冒号拼接
TEMPERATURE = 0.2
MAX_TOKENS = 1024
```

### 3. 运行

#### 固定示例（快速体验）

bash

```
python app.py
```



#### 交互式控制台

bash

```
python console_demo.py
```



#### 运行测试用例

bash

```
python test_console.py
```



## 输出文件

运行后会在 `data/resources/{学生ID}/` 生成：

- `profile.json` – 8 个维度的学生画像
- `resources.json` – 5 类学习资源的完整内容
- `learning_path.json` – 5 步学习路径
- `mindmap.mmd` – Mermaid 思维导图代码
- `code_practice.py` – 可直接运行的 Python 代码

## 项目结构

school_ai_system/
├── school_agent/           # 核心代码包
│   ├── agent.py           # LangGraph 图定义
│   ├── nodes.py           # 各节点逻辑
│   ├── state.py           # 状态定义
│   ├── guards.py          # 数据校验与修复
│   ├── kb.py              # 知识库检索
│   └── config.py          # 模型配置
├── data/                  # 运行时数据
│   ├── knowledge_base/    # 知识库文档
│   └── resources/         # 生成的学生资源
├── app.py                 # 固定输入示例
├── console_demo.py        # 交互式控制台
├── test_console.py        # 自动化测试
├── requirements.txt
└── README.md