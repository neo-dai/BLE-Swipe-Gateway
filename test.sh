#!/bin/bash

# MBBridge Controller 测试脚本
# 使用方法：./test.sh [device_id]
#
# 示例：
#   ./test.sh                    # 自动连接设备
#   ./test.sh emulator-5554     # 指定设备

DEVICE_ID=${1:-""}
ADB="adb"
if [ -n "$DEVICE_ID" ]; then
    ADB="adb -s $DEVICE_ID"
fi

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "MBBridge Controller 测试脚本"
echo "======================================"

# 检查设备连接
echo -e "\n${YELLOW}检查设备连接...${NC}"
$ADB devices
echo ""

# 测试健康检查
echo -e "${YELLOW}1. 测试健康检查...${NC}"
HEALTH_RESULT=$($ADB shell curl -s http://127.0.0.1:27123/health)
echo "响应: $HEALTH_RESULT"

if echo "$HEALTH_RESULT" | grep -q '"ok":1'; then
    echo -e "${GREEN}✓ 健康检查通过${NC}"
else
    echo -e "${RED}✗ 健康检查失败（服务器可能未启动）${NC}"
    echo "请先在 App 中启动服务器"
    exit 1
fi

# 测试 PREV 命令
echo -e "\n${YELLOW}2. 测试 PREV 命令...${NC}"
PREV_CODE=$($ADB shell 'printf "\x01" | curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://127.0.0.1:27123/cmd -H "Content-Type: application/octet-stream" --data-binary @-')
echo "HTTP: $PREV_CODE"

if [ "$PREV_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PREV 命令成功${NC}"
else
    echo -e "${RED}✗ PREV 命令失败${NC}"
fi

sleep 1

# 测试 NEXT 命令
echo -e "\n${YELLOW}3. 测试 NEXT 命令...${NC}"
NEXT_CODE=$($ADB shell 'printf "\x02" | curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://127.0.0.1:27123/cmd -H "Content-Type: application/octet-stream" --data-binary @-')
echo "HTTP: $NEXT_CODE"

if [ "$NEXT_CODE" = "200" ]; then
    echo -e "${GREEN}✓ NEXT 命令成功${NC}"
else
    echo -e "${RED}✗ NEXT 命令失败${NC}"
fi

# 测试错误格式
echo -e "\n${YELLOW}4. 测试错误格式...${NC}"
INVALID_CODE=$($ADB shell 'curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://127.0.0.1:27123/cmd -H "Content-Type: application/octet-stream" --data-binary ""')
echo "HTTP: $INVALID_CODE"

if [ "$INVALID_CODE" = "400" ]; then
    echo -e "${GREEN}✓ 正确拒绝了无效格式${NC}"
else
    echo -e "${RED}✗ 应该拒绝无效格式${NC}"
fi

# 测试 404
echo -e "\n${YELLOW}5. 测试 404 Not Found...${NC}"
NOTFOUND_RESULT=$($ADB shell curl -s http://127.0.0.1:27123/notfound)
echo "响应: $NOTFOUND_RESULT"

if echo "$NOTFOUND_RESULT" | grep -q '"ok":0'; then
    echo -e "${GREEN}✓ 正确返回 404${NC}"
else
    echo -e "${RED}✗ 应该返回 404${NC}"
fi

# 测试 Token 认证（需要先在 App 中设置 Token）
echo -e "\n${YELLOW}6. 测试 Token 认证（可选）${NC}"
echo "如果已在 App 中设置了 Token，将测试 Token 验证"
echo -n "请输入要测试的 Token（留空跳过）："
read -r TEST_TOKEN

if [ -n "$TEST_TOKEN" ]; then
    echo -e "\n测试正确 Token..."
    AUTH_CODE=$($ADB shell 'printf "\x01" | curl -s -o /dev/null -w "%{http_code}" \
      -X POST http://127.0.0.1:27123/cmd -H "Content-Type: application/octet-stream" \
      -H "X-MBBridge-Token: '"$TEST_TOKEN"'" --data-binary @-')
    echo "HTTP: $AUTH_CODE"

    if [ "$AUTH_CODE" = "200" ]; then
        echo -e "${GREEN}✓ Token 验证通过${NC}"
    else
        echo -e "${RED}✗ Token 验证失败${NC}"
    fi

    echo -e "\n测试错误 Token..."
    WRONG_CODE=$($ADB shell 'printf "\x01" | curl -s -o /dev/null -w "%{http_code}" \
      -X POST http://127.0.0.1:27123/cmd -H "Content-Type: application/octet-stream" \
      -H "X-MBBridge-Token: wrong_token" --data-binary @-')
    echo "HTTP: $WRONG_CODE"

    if [ "$WRONG_CODE" = "401" ]; then
        echo -e "${GREEN}✓ 正确拒绝了错误 Token${NC}"
    else
        echo -e "${RED}✗ 应该拒绝错误 Token${NC}"
    fi
else
    echo -e "${YELLOW}跳过 Token 测试${NC}"
fi

echo -e "\n======================================"
echo -e "${GREEN}测试完成！${NC}"
echo "======================================"
echo ""
echo "请检查 App 中的："
echo "  - 统计计数是否增加"
echo "  - 最近命令是否更新"
echo "  - 日志是否记录"
