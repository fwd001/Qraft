#!/bin/bash

# QRCodeKit 测试运行脚本
# 位置: /Users/wendongfu/Documents/code/QRCodeKit/test_runner.sh

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "========================================"
echo "  QRCodeKit 测试运行脚本"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否在正确目录
if [ ! -f "settings.gradle.kts" ]; then
    echo -e "${RED}错误: 找不到 settings.gradle.kts，请确保在项目根目录运行此脚本${NC}"
    exit 1
fi

# 解析参数
TEST_TYPE=""
TEST_FILTER=""
REPORT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        unit)
            TEST_TYPE="unit"
            shift
            ;;
        ui|android)
            TEST_TYPE="ui"
            shift
            ;;
        all)
            TEST_TYPE="all"
            shift
            ;;
        --filter)
            TEST_FILTER="$2"
            shift 2
            ;;
        --report)
            REPORT=true
            shift
            ;;
        --help|-h)
            echo "用法: ./test_runner.sh [unit|ui|all] [选项]"
            echo ""
            echo "选项:"
            echo "  unit          只运行单元测试"
            echo "  ui           只运行 UI 测试"
            echo "  all          运行所有测试 (默认)"
            echo "  --filter X   只运行匹配 X 的测试"
            echo "  --report     生成测试报告"
            echo "  --help, -h   显示此帮助信息"
            exit 0
            ;;
        *)
            echo -e "${RED}未知参数: $1${NC}"
            echo "使用 --help 查看帮助"
            exit 1
            ;;
    esac
done

TEST_TYPE="${TEST_TYPE:-all}"

echo -e "${YELLOW}测试类型: ${TEST_TYPE}${NC}"
if [ -n "$TEST_FILTER" ]; then
    echo -e "${YELLOW}测试过滤器: ${TEST_FILTER}${NC}"
fi
echo ""

# 单元测试命令
run_unit_tests() {
    echo -e "${GREEN}>>> 运行单元测试...${NC}"
    ./gradlew testDebugUnitTest \
        --no-daemon \
        --stacktrace \
        $([ -n "$TEST_FILTER" ] && echo "--tests '$TEST_FILTER'" || true)
}

# UI 测试命令
run_ui_tests() {
    echo -e "${GREEN}>>> 运行 UI 测试...${NC}"
    ./gradlew connectedDebugAndroidTest \
        --no-daemon \
        --stacktrace \
        $([ -n "$TEST_FILTER" ] && echo "--tests '$TEST_FILTER'" || true)
}

# 生成测试报告
generate_report() {
    echo -e "${GREEN}>>> 生成测试报告...${NC}"
    ./gradlew testDebugUnitTestReport \
        connectedDebugAndroidTestReport \
        --no-daemon

    echo ""
    echo -e "${GREEN}测试报告位置:${NC}"
    echo "  单元测试: app/build/reports/tests/testDebugUnitTest/index.html"
    echo "  UI 测试:  app/build/reports/androidTests/connectedDebugAndroidTest/index.html"
}

# 运行测试
case $TEST_TYPE in
    unit)
        run_unit_tests
        ;;
    ui)
        run_ui_tests
        ;;
    all)
        run_unit_tests
        echo ""
        echo "-----------------------------------"
        echo ""
        run_ui_tests
        ;;
esac

# 生成报告 (可选)
if [ "$REPORT" = true ]; then
    generate_report
fi

echo ""
echo "========================================"
echo -e "${GREEN}测试运行完成!${NC}"
echo "========================================"
