# edu-agent-ai

面向高校课程个性化学习的多智能体系统示例工程。

## 目录分层

- `school_agent/graph.py`：LangGraph 总控流程。
- `school_agent/agents/`：每个智能体一个文件。
- `school_agent/services/`：大模型调用、画像存储、资源保存、日志等公共服务。
- `school_agent/kb/`：课程知识库加载与检索。
- `school_agent/prompts/`：各智能体 Prompt 模板。
- `data/`：课程知识库、学生画像、学习日志和生成资源。
- `tests/`：基础流程测试。

## 快速运行

```bash
pip install -r requirements.txt
cp .env.example .env
python run_demo.py
```

没有模型 Key 时，系统默认使用 `USE_MOCK_LLM=1` 的本地模拟输出，保证总控流程可以跑通。

## 组内开发约定

1. `graph.py` 只写流程编排，不写具体业务生成逻辑。
2. `agents/` 下每个智能体一个文件。
3. 每个智能体主函数统一接收 `state`，返回 `dict`。
4. 大模型调用统一走 `services/llm_client.py`。
5. Prompt 统一放在 `prompts/`。
6. 数据读写统一走 `services/`。
7. 知识库检索统一走 `kb/retriever.py`。
