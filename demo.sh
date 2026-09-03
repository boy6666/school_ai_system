#!/bin/bash

# =============================================
# EduAgent 学生侧资源演示脚本（最终版）
# =============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
BASE_URL="http://localhost:8888"
API_PREFIX="/api/edu-agent-resource"

check_service() {
    echo -e "${YELLOW}检查服务状态...${NC}"
    if curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${API_PREFIX}" | grep -q 200; then
        echo -e "${GREEN}✅ 服务正常运行${NC}"
        return 0
    else
        echo -e "${RED}❌ 服务未启动${NC}"
        return 1
    fi
}

hr() { echo "============================================================"; }

test_generate() {
    hr
    echo -e "${YELLOW}��� 1. 生成新资源（异步）${NC}"
    echo "请求：POST ${API_PREFIX}/generate"
    echo '{"userId":1,"chapter":"demo","chapterId":"ch001","topic":"HashMap","type":"mindmap","difficulty":"easy"}'

    RESPONSE=$(curl -s -X POST "${BASE_URL}${API_PREFIX}/generate" \
        -H "Content-Type: application/json" \
        -d '{"userId":1,"chapter":"demo","chapterId":"ch001","topic":"HashMap","type":"mindmap","difficulty":"easy"}')

    echo "响应："
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"

    ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -n "$ID" ]; then
        echo -e "${GREEN}✅ 资源已创建，ID=${ID}${NC}"
        echo "$ID" > /tmp/resource_id.txt
    else
        echo -e "${RED}❌ 生成失败${NC}"
        return 1
    fi
}

poll_resource() {
    hr
    echo -e "${YELLOW}⏳ 2. 轮询资源状态${NC}"
    ID=$(cat /tmp/resource_id.txt 2>/dev/null)
    if [ -z "$ID" ]; then
        echo -e "${RED}❌ 没有资源ID${NC}"
        return 1
    fi
    for i in {1..10}; do
        STATUS=$(curl -s "${BASE_URL}${API_PREFIX}/${ID}" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$STATUS" == "published" ] || [ "$STATUS" == "failed" ]; then
            echo -e "${GREEN}✅ 状态变为 ${STATUS}（尝试 ${i} 次）${NC}"
            curl -s "${BASE_URL}${API_PREFIX}/${ID}" | jq '.' 2>/dev/null
            return 0
        fi
        echo "当前状态：${STATUS}（等待中...）"
        sleep 1
    done
    echo -e "${RED}❌ 超时${NC}"
    return 1
}

test_list() {
    hr
    echo -e "${YELLOW}��� 3. 资源列表${NC}"
    curl -s "${BASE_URL}${API_PREFIX}" | jq '.' 2>/dev/null || curl -s "${BASE_URL}${API_PREFIX}"
}

test_chapter() {
    hr
    echo -e "${YELLOW}��� 4. 按章节查询（chapterId=ch001, type=mindmap）${NC}"
    curl -s "${BASE_URL}${API_PREFIX}/chapter/ch001/mindmap" | jq '.' 2>/dev/null
}

test_favorite() {
    hr
    echo -e "${YELLOW}⭐ 5. 收藏资源（ID=${1}）${NC}"
    # 使用 @RequestParam 方式传递 favorite 参数
    curl -s -X POST "${BASE_URL}${API_PREFIX}/${1}/favorite?favorite=true" | jq '.' 2>/dev/null
    echo -e "${GREEN}✅ 收藏请求已发送${NC}"
}

test_favorites() {
    hr
    echo -e "${YELLOW}❤️ 6. 我的收藏列表${NC}"
    curl -s "${BASE_URL}${API_PREFIX}/favorites/mine" | jq '.' 2>/dev/null
}

test_feedback() {
    hr
    echo -e "${YELLOW}��� 7. 提交反馈（ID=${1}）${NC}"
    # 使用 @RequestParam 方式传递反馈参数
    curl -s -X POST "${BASE_URL}${API_PREFIX}/${1}/feedback?liked=true&difficultyFeedback=适中" | jq '.' 2>/dev/null
    echo -e "${GREEN}✅ 反馈已提交${NC}"
}

main() {
    check_service || exit 1
    test_generate || exit 1
    ID=$(cat /tmp/resource_id.txt)
    poll_resource || exit 1
    test_list
    test_chapter
    test_favorite "$ID"
    test_favorites
    test_feedback "$ID"
    hr
    echo -e "${GREEN}��� 演示完成！${NC}"
    echo "清理：curl -X DELETE ${BASE_URL}${API_PREFIX}/${ID}"
}

main
