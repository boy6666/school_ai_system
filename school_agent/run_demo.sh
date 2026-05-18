#!/bin/bash

echo "====================================="
echo "个性化学习智能体 - 一键启动脚本"
echo "====================================="

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到 python3，请先安装 Python 3.10+"
    exit 1
fi

# 检查依赖
echo "正在检查依赖..."
if ! pip3 show langgraph &> /dev/null; then
    echo "正在安装依赖..."
    pip3 install -r requirements.txt
fi

# 检查 .env 或 config.py 是否配置了 API 密钥
if [ ! -f .env ] && ! grep -q "API_KEY.*sk-" school_agent/config.py 2>/dev/null; then
    echo "警告: 未检测到 API 密钥配置。"
    echo "请编辑 school_agent/config.py 或创建 .env 文件，设置正确的 API_KEY 和 BASE_URL。"
    read -p "按回车继续尝试运行（可能会因 API 错误而失败）..."
fi

# 运行控制台演示
echo "启动控制台演示..."
python3 console_demo.py

使用说明：

在 Linux/macOS 下执行 chmod +x run_demo.sh 后运行 ./run_demo.sh

Windows 用户可直接运行 python console_demo.py