#!/usr/bin/env python3
"""
MBBridge Controller Python 测试脚本

使用方法：
1. 通过 USB 连接 Android 设备
2. 启动 App 并启动服务器
3. 运行脚本：python3 test.py

依赖安装：
pip install requests

或使用 ADB 端口转发后直接测试：
adb forward tcp:27123 tcp:27123
python3 test.py
"""

import requests
import time
import sys

# 配置
HOST = "127.0.0.1"
PORT = 27123
BASE_URL = f"http://{HOST}:{PORT}"

# 颜色输出
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    END = '\033[0m'

def print_test(test_name):
    print(f"\n{Colors.YELLOW}{test_name}{Colors.END}")

def print_success(message):
    print(f"{Colors.GREEN}✓ {message}{Colors.END}")

def print_error(message):
    print(f"{Colors.RED}✗ {message}{Colors.END}")

def test_health():
    """测试健康检查"""
    print_test("1. 测试健康检查...")
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=2)
        data = response.json()

        print(f"响应: {json.dumps(data, indent=2)}")

        if data.get('ok') == 1 and data.get('app') == 'MBBridgeCtrl':
            print_success("健康检查通过")
            return True
        else:
            print_error("健康检查失败")
            return False
    except Exception as e:
        print_error(f"健康检查异常: {e}")
        print("提示：请确保 App 已启动服务器，或运行: adb forward tcp:27123 tcp:27123")
        return False

def test_prev_command():
    """测试 PREV 命令"""
    print_test("2. 测试 PREV 命令...")
    try:
        response = requests.post(
            f"{BASE_URL}/cmd",
            headers={"Content-Type": "application/octet-stream"},
            data=bytes([1]),
            timeout=2
        )
        print("请求: v=1")
        print(f"HTTP: {response.status_code}")

        if response.status_code == 200:
            print_success("PREV 命令成功")
            return True
        else:
            print_error("PREV 命令失败")
            return False
    except Exception as e:
        print_error(f"PREV 命令异常: {e}")
        return False

def test_next_command():
    """测试 NEXT 命令"""
    print_test("3. 测试 NEXT 命令...")
    try:
        response = requests.post(
            f"{BASE_URL}/cmd",
            headers={"Content-Type": "application/octet-stream"},
            data=bytes([2]),
            timeout=2
        )
        print("请求: v=2")
        print(f"HTTP: {response.status_code}")

        if response.status_code == 200:
            print_success("NEXT 命令成功")
            return True
        else:
            print_error("NEXT 命令失败")
            return False
    except Exception as e:
        print_error(f"NEXT 命令异常: {e}")
        return False

def test_invalid_format():
    """测试错误格式"""
    print_test("4. 测试错误格式...")
    try:
        response = requests.post(
            f"{BASE_URL}/cmd",
            headers={"Content-Type": "application/octet-stream"},
            data=b"",
            timeout=2
        )
        print("请求: empty body")
        print(f"HTTP: {response.status_code}")

        if response.status_code == 400:
            print_success("正确拒绝了无效格式")
            return True
        else:
            print_error("应该拒绝无效格式")
            return False
    except Exception as e:
        print_error(f"测试异常: {e}")
        return False

def test_404():
    """测试 404"""
    print_test("5. 测试 404 Not Found...")
    try:
        response = requests.get(f"{BASE_URL}/notfound", timeout=2)
        result = response.json()

        print(f"响应: {json.dumps(result)}")

        if result.get('ok') == 0:
            print_success("正确返回 404")
            return True
        else:
            print_error("应该返回 404")
            return False
    except Exception as e:
        print_error(f"测试异常: {e}")
        return False

def test_token_auth(token):
    """测试 Token 认证"""
    if not token:
        print_test("6. 跳过 Token 测试")
        return True

    print_test("6. 测试 Token 认证...")

    # 测试正确 Token
    print("\n测试正确 Token...")
    try:
        response = requests.post(
            f"{BASE_URL}/cmd",
            headers={
                "Content-Type": "application/octet-stream",
                "X-MBBridge-Token": token
            },
            data=bytes([1]),
            timeout=2
        )
        if response.status_code == 200:
            print_success("Token 验证通过")
            token_ok = True
        else:
            print_error("Token 验证失败")
            token_ok = False
    except Exception as e:
        print_error(f"Token 测试异常: {e}")
        token_ok = False

    # 测试错误 Token
    print("\n测试错误 Token...")
    try:
        response = requests.post(
            f"{BASE_URL}/cmd",
            headers={
                "Content-Type": "application/octet-stream",
                "X-MBBridge-Token": "wrong_token"
            },
            data=bytes([1]),
            timeout=2
        )

        if response.status_code == 401 or 'Unauthorized' in response.text:
            print_success("正确拒绝了错误 Token")
            wrong_token_ok = True
        else:
            print_error("应该拒绝错误 Token")
            wrong_token_ok = False
    except Exception as e:
        print_error(f"测试异常: {e}")
        wrong_token_ok = False

    return token_ok and wrong_token_ok

def stress_test(count=10):
    """压力测试"""
    print_test(f"7. 压力测试（发送 {count} 个命令）...")

    success_count = 0
    start_time = time.time()

    for i in range(count):
        try:
            cmd = 1 if i % 2 == 0 else 2
            response = requests.post(
                f"{BASE_URL}/cmd",
                headers={"Content-Type": "application/octet-stream"},
                data=bytes([cmd]),
                timeout=2
            )
            if response.status_code == 200:
                success_count += 1
        except Exception as e:
            print_error(f"第 {i+1} 个命令失败: {e}")

    elapsed = time.time() - start_time
    print(f"\n完成: {success_count}/{count} 成功")
    print(f"耗时: {elapsed:.2f} 秒")
    print(f"平均: {1000*elapsed/count:.2f} ms/请求")

    if success_count == count:
        print_success("压力测试通过")
        return True
    else:
        print_error("部分命令失败")
        return False

def main():
    print("=" * 50)
    print("MBBridge Controller 测试脚本")
    print("=" * 50)

    # 检查 Token
    token = input("\n请输入要测试的 Token（留空跳过 Token 测试）: ").strip()

    # 运行测试
    results = []
    results.append(test_health())

    if results[-1]:  # 健康检查通过才继续
        time.sleep(0.5)
        results.append(test_prev_command())

        time.sleep(0.5)
        results.append(test_next_command())

        time.sleep(0.5)
        results.append(test_invalid_format())

        time.sleep(0.5)
        results.append(test_404())

        results.append(test_token_auth(token))

        # 可选：压力测试
        stress = input("\n是否运行压力测试？(y/n): ").strip().lower()
        if stress == 'y':
            results.append(stress_test(10))

    # 总结
    print("\n" + "=" * 50)
    total = len(results)
    passed = sum(results)

    if passed == total:
        print(f"{Colors.GREEN}所有测试通过！({passed}/{total}){Colors.END}")
    else:
        print(f"{Colors.YELLOW}部分测试失败 ({passed}/{total}){Colors.END}")

    print("=" * 50)
    print("\n请检查 App 中的：")
    print("  - 统计计数是否增加")
    print("  - 最近命令是否更新")
    print("  - 日志是否记录")

    return 0 if passed == total else 1

if __name__ == "__main__":
    sys.exit(main())
