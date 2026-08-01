FROM python:3.11-slim
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# 默认 mock LLM（无 API_KEY 也能跑通 demo / 本地联调）；接真实模型时通过环境变量覆盖
ENV USE_MOCK_LLM=1

EXPOSE 8001
# api.py 中 app = FastAPI(...) 位于模块顶层，故 uvicorn 以 api:app 启动
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8001"]
