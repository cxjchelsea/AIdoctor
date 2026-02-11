#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AIdoctor 项目一键启动脚本
支持选择要启动的服务，适配多种技术栈（Python、Java、前端）
对于Python服务支持使用conda环境
"""

import os
import sys
import json
import subprocess
import platform
import shutil
import copy
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# 项目根目录（脚本在scripts文件夹中，需要向上两级到项目根目录）
SCRIPT_DIR = Path(__file__).resolve().parent
# 如果脚本在scripts文件夹中，向上两级；否则就是当前目录
if SCRIPT_DIR.name == "scripts":
    PROJECT_ROOT = SCRIPT_DIR.parent
else:
    PROJECT_ROOT = SCRIPT_DIR

# 默认服务配置（作为后备）
DEFAULT_SERVICES_CONFIG = {
    "python": {
        "clinical-parsing-service": {
            "name": "临床解析服务",
            "path": "clinical-parsing-service",
            "port": 8082,
            "run_file": "run.py",
            "conda_env": None,  # 如果为None，会尝试自动检测或使用默认环境
        },
        "dialog-service": {
            "name": "对话服务",
            "path": "dialog-service",
            "port": 8088,
            "run_file": "run.py",
            "conda_env": None,
        },
        "diagnosis-engine-service": {
            "name": "诊断引擎服务",
            "path": "diagnosis-engine-service",
            "port": 8086,
            "run_file": "run.py",
            "conda_env": None,
        },
        "explanation-service": {
            "name": "解释服务",
            "path": "explanation-service",
            "port": 8089,
            "run_file": "run.py",
            "conda_env": None,
        },
        "health-state-assessment-service": {
            "name": "健康状态评估服务",
            "path": "health-state-assessment-service",
            "port": 8093,
            "run_file": "run.py",
            "conda_env": None,
        },
        "ocr-service": {
            "name": "OCR服务",
            "path": "ocr-service",
            "port": 8087,
            "run_file": "run.py",
            "conda_env": None,
        },
        "risk-assessment-service": {
            "name": "风险评估服务",
            "path": "risk-assessment-service",
            "port": 8092,
            "run_file": "run.py",
            "conda_env": None,
        },
        "treatment-engine-service": {
            "name": "治疗推理服务",
            "path": "treatment-engine-service",
            "port": 8091,
            "run_file": "run.py",
            "conda_env": None,
        },
        "workup-planner-service": {
            "name": "检查建议服务",
            "path": "workup-planner-service",
            "port": 8090,
            "run_file": "run.py",
            "conda_env": None,
        },
    },
    "java": {
        "diagnosis-service": {
            "name": "诊断服务",
            "path": "diagnosis-service",
            "port": 8084,
            "maven_cmd": "spring-boot:run",
        },
        "examination-service": {
            "name": "检查服务",
            "path": "examination-service",
            "port": 8085,
            "maven_cmd": "spring-boot:run",
        },
        "execution-trace-service": {
            "name": "执行追踪服务",
            "path": "execution-trace-service",
            "port": 8083,
            "maven_cmd": "spring-boot:run",
        },
    },
    "frontend": {
        "frontend": {
            "name": "前端服务",
            "path": "frontend",
            "port": 5173,
            "npm_cmd": "dev",
        },
        "frontend-admin": {
            "name": "管理后台",
            "path": "frontend-admin",
            "port": 5174,
            "npm_cmd": "dev",
        },
    },
}

# 服务配置（将从配置文件加载）
SERVICES_CONFIG = None

# 存储运行中的进程
running_processes: List[subprocess.Popen] = []


def load_services_config() -> Dict:
    """从配置文件加载服务配置"""
    config_file = SCRIPT_DIR / "services_config.json"
    
    # 先使用默认配置（深拷贝以避免修改原始配置）
    config = {
        "python": {k: copy.deepcopy(v) for k, v in DEFAULT_SERVICES_CONFIG["python"].items()},
        "java": {k: copy.deepcopy(v) for k, v in DEFAULT_SERVICES_CONFIG["java"].items()},
        "frontend": {k: copy.deepcopy(v) for k, v in DEFAULT_SERVICES_CONFIG["frontend"].items()},
    }
    
    # 如果配置文件存在，则加载并合并
    if config_file.exists():
        try:
            with open(config_file, "r", encoding="utf-8") as f:
                file_config = json.load(f)
            
            # 合并Python服务配置
            if "python_services" in file_config:
                for service_key, service_config in file_config["python_services"].items():
                    if service_key in config["python"]:
                        # 更新配置，保留默认值作为后备
                        config["python"][service_key].update({
                            "name": service_config.get("name", config["python"][service_key]["name"]),
                            "port": service_config.get("port", config["python"][service_key]["port"]),
                        })
                        # 更新conda_env（如果配置文件中指定了）
                        if "conda_env" in service_config:
                            conda_env_value = service_config["conda_env"]
                            # 如果配置为null或空字符串，则使用None
                            config["python"][service_key]["conda_env"] = conda_env_value if conda_env_value else None
            
            # 合并Java服务配置
            if "java_services" in file_config:
                for service_key, service_config in file_config["java_services"].items():
                    if service_key in config["java"]:
                        config["java"][service_key].update({
                            "name": service_config.get("name", config["java"][service_key]["name"]),
                            "port": service_config.get("port", config["java"][service_key]["port"]),
                        })
            
            # 合并前端服务配置
            if "frontend_services" in file_config:
                for service_key, service_config in file_config["frontend_services"].items():
                    if service_key in config["frontend"]:
                        config["frontend"][service_key].update({
                            "name": service_config.get("name", config["frontend"][service_key]["name"]),
                            "port": service_config.get("port", config["frontend"][service_key]["port"]),
                        })
            
            print_colored(f"已从配置文件加载服务配置: {config_file}", "green")
        except Exception as e:
            print_colored(f"警告: 加载配置文件失败，使用默认配置: {e}", "yellow")
    else:
        print_colored(f"提示: 未找到配置文件 {config_file}，使用默认配置", "yellow")
    
    return config


def print_colored(text: str, color: str = "white"):
    """打印彩色文本（Windows和Linux都支持）"""
    colors = {
        "red": "\033[91m",
        "green": "\033[92m",
        "yellow": "\033[93m",
        "blue": "\033[94m",
        "magenta": "\033[95m",
        "cyan": "\033[96m",
        "white": "\033[97m",
        "reset": "\033[0m",
    }
    
    if platform.system() == "Windows":
        # Windows 10+ 支持ANSI转义序列
        try:
            import ctypes
            kernel32 = ctypes.windll.kernel32
            kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
        except:
            pass
    
    print(f"{colors.get(color, '')}{text}{colors.get('reset', '')}")


def check_command(command: str) -> bool:
    """检查命令是否可用"""
    return shutil.which(command) is not None


def find_conda_env() -> Optional[str]:
    """查找可用的conda环境"""
    # 首先检查环境变量（最可靠的方式）
    conda_env = os.environ.get("CONDA_DEFAULT_ENV")
    if conda_env and conda_env != "base":
        return conda_env
    
    # 检查conda命令
    if check_command("conda"):
        try:
            # 尝试获取conda信息
            result = subprocess.run(
                ["conda", "info", "--envs"],
                capture_output=True,
                text=True,
                timeout=5,
                shell=(platform.system() == "Windows")
            )
            if result.returncode == 0:
                # 解析输出，查找激活的环境（标记为*的环境）
                for line in result.stdout.split("\n"):
                    line = line.strip()
                    if "*" in line:
                        parts = line.split()
                        if len(parts) > 0:
                            env_name = parts[0]
                            # 跳过base环境，除非没有其他环境
                            if env_name != "base":
                                return env_name
                            elif conda_env is None:
                                return "base"
        except Exception as e:
            # 静默失败，继续其他检测方式
            pass
    
    # 如果环境变量中有base环境，返回它
    if conda_env == "base":
        return "base"
    
    return None


def find_conda_python(conda_env: str) -> Optional[str]:
    """查找conda环境中的Python可执行文件路径"""
    if platform.system() == "Windows":
        # Windows: 尝试多种方式找到conda Python
        # 方式1: 尝试从CONDA_PREFIX环境变量获取
        conda_base = os.environ.get("CONDA_PREFIX")
        if conda_base:
            # 检查当前环境是否匹配
            env_name = os.path.basename(conda_base)
            if env_name == conda_env:
                python_exe = os.path.join(conda_base, "python.exe")
                if os.path.exists(python_exe):
                    return python_exe
        
        # 方式2: 从CONDA_DEFAULT_ENV和CONDA_PREFIX推断
        conda_default_env = os.environ.get("CONDA_DEFAULT_ENV")
        if conda_default_env == conda_env and conda_base:
            python_exe = os.path.join(conda_base, "python.exe")
            if os.path.exists(python_exe):
                return python_exe
        
        # 方式3: 尝试从CONDA_EXE推断conda根目录
        conda_exe = os.environ.get("CONDA_EXE")
        if conda_exe:
            # conda_exe通常是 .../Scripts/conda.exe 或 .../conda.exe
            conda_root = os.path.dirname(os.path.dirname(conda_exe))
            python_exe = os.path.join(conda_root, "envs", conda_env, "python.exe")
            if os.path.exists(python_exe):
                return python_exe
        
        # 方式4: 使用conda info命令获取所有环境路径（最可靠）
        if check_command("conda"):
            try:
                result = subprocess.run(
                    ["conda", "info", "--envs"],
                    capture_output=True,
                    text=True,
                    timeout=5,
                    shell=True
                )
                if result.returncode == 0:
                    # 解析输出，查找目标环境
                    for line in result.stdout.split("\n"):
                        line = line.strip()
                        if not line or line.startswith("#"):
                            continue
                        # 格式通常是: env_name  path 或 *env_name  path
                        parts = line.split()
                        if len(parts) >= 2:
                            env_name = parts[0].lstrip("*")
                            env_path = parts[-1]
                            if env_name == conda_env:
                                python_exe = os.path.join(env_path, "python.exe")
                                if os.path.exists(python_exe):
                                    return python_exe
            except Exception:
                # 静默失败，继续其他检测方式
                pass
        
        # 方式5: 尝试常见的conda安装路径
        username = os.environ.get("USERNAME", os.environ.get("USER", ""))
        conda_paths = [
            os.path.join(os.path.expanduser("~"), "anaconda3", "envs", conda_env, "python.exe"),
            os.path.join(os.path.expanduser("~"), "miniconda3", "envs", conda_env, "python.exe"),
            os.path.join("C:", "Users", username, "anaconda3", "envs", conda_env, "python.exe"),
            os.path.join("C:", "Users", username, "miniconda3", "envs", conda_env, "python.exe"),
            os.path.join("C:", "ProgramData", "anaconda3", "envs", conda_env, "python.exe"),
            os.path.join("C:", "ProgramData", "miniconda3", "envs", conda_env, "python.exe"),
            # 添加E盘路径（常见安装位置）
            os.path.join("E:", "Anaconda3", "envs", conda_env, "python.exe"),
            os.path.join("E:", "anaconda3", "envs", conda_env, "python.exe"),
            os.path.join("E:", "Miniconda3", "envs", conda_env, "python.exe"),
            os.path.join("E:", "miniconda3", "envs", conda_env, "python.exe"),
            os.path.join("D:", "Anaconda3", "envs", conda_env, "python.exe"),
            os.path.join("D:", "anaconda3", "envs", conda_env, "python.exe"),
        ]
        
        for conda_python in conda_paths:
            if os.path.exists(conda_python):
                return conda_python
    else:
        # Linux/Mac: 尝试查找conda环境
        conda_base = os.environ.get("CONDA_PREFIX")
        if conda_base:
            env_name = os.path.basename(conda_base)
            if env_name == conda_env:
                python_exe = os.path.join(conda_base, "bin", "python")
                if os.path.exists(python_exe):
                    return python_exe
        
        # 尝试从CONDA_EXE推断
        conda_exe = os.environ.get("CONDA_EXE")
        if conda_exe:
            conda_root = os.path.dirname(os.path.dirname(conda_exe))
            python_exe = os.path.join(conda_root, "envs", conda_env, "bin", "python")
            if os.path.exists(python_exe):
                return python_exe
        
        # 尝试常见路径
        home = os.path.expanduser("~")
        conda_paths = [
            os.path.join(home, "anaconda3", "envs", conda_env, "bin", "python"),
            os.path.join(home, "miniconda3", "envs", conda_env, "bin", "python"),
        ]
        
        for conda_python in conda_paths:
            if os.path.exists(conda_python):
                return conda_python
    
    return None


def get_python_command(service_config: Dict, conda_env: Optional[str] = None) -> List[str]:
    """获取Python命令（支持conda环境）"""
    if conda_env:
        # 优先尝试直接找到conda环境的Python可执行文件（避免conda run的并发问题）
        python_exe = find_conda_python(conda_env)
        if python_exe:
            return [python_exe]
        
        # 如果找不到，尝试使用conda run（作为后备方案，但会有并发问题）
        # 注意：在Windows上，多个服务同时使用conda run会导致临时文件冲突
        if check_command("conda"):
            print_colored(f"警告: 无法直接找到conda环境 {conda_env} 的Python，将使用conda run（可能导致并发问题）", "yellow")
            if platform.system() == "Windows":
                return ["conda", "run", "--no-capture-output", "-n", conda_env, "python"]
            else:
                return ["conda", "run", "--no-capture-output", "-n", conda_env, "python"]
        else:
            print_colored(f"错误: 未找到conda命令，且无法找到conda环境 {conda_env} 的Python", "red")
    
    # 使用系统Python
    return [sys.executable]


def start_python_service(service_key: str, service_config: Dict, conda_env: Optional[str] = None) -> subprocess.Popen:
    """启动Python服务"""
    service_path = PROJECT_ROOT / service_config["path"]
    run_file = service_path / service_config["run_file"]
    
    if not run_file.exists():
        print_colored(f"错误: 找不到启动文件 {run_file}", "red")
        return None
    
    python_cmd = get_python_command(service_config, conda_env)
    cmd = python_cmd + [str(run_file)]
    
    print_colored(f"启动 {service_config['name']} (端口: {service_config['port']})...", "cyan")
    if conda_env:
        print_colored(f"  使用conda环境: {conda_env}", "yellow")
    
    # 设置工作目录
    env = os.environ.copy()
    
    # Windows和Linux处理方式不同
    if platform.system() == "Windows":
        # Windows: 使用start命令创建新的cmd窗口
        # 对于包含空格的路径，需要特殊处理
        def escape_cmd_arg(arg):
            """转义命令参数"""
            if " " in arg or '"' in arg:
                return f'"{arg.replace('"', '\\"')}"'
            return arg
        
        # 构建完整的命令字符串
        cmd_str = " ".join(escape_cmd_arg(arg) for arg in cmd)
        # 使用start命令在新窗口中运行，确保输出可见
        # /k 保持窗口打开，echo off 避免显示命令本身
        full_cmd = f'cmd /c start "{service_config["name"]}" cmd /k "cd /d "{service_path}" && echo 正在启动服务... && {cmd_str}"'
        process = subprocess.Popen(
            full_cmd,
            cwd=str(service_path),
            env=env,
            shell=True,
        )
    else:
        # Linux/Mac: 后台运行
        process = subprocess.Popen(
            cmd,
            cwd=str(service_path),
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    
    return process


def start_java_service(service_key: str, service_config: Dict) -> subprocess.Popen:
    """启动Java服务"""
    service_path = PROJECT_ROOT / service_config["path"]
    pom_file = service_path / "pom.xml"
    
    if not pom_file.exists():
        print_colored(f"错误: 找不到pom.xml文件 {pom_file}", "red")
        return None
    
    # 查找Maven命令（Windows上可能是mvn.cmd）
    mvn_command = None
    if platform.system() == "Windows":
        # Windows: 尝试查找mvn.cmd或mvn.bat
        for cmd in ["mvn.cmd", "mvn.bat", "mvn"]:
            if check_command(cmd):
                mvn_command = cmd
                break
    else:
        # Linux/Mac: 使用mvn
        if check_command("mvn"):
            mvn_command = "mvn"
    
    if not mvn_command:
        print_colored("错误: 未找到Maven命令，请确保Maven已安装并在PATH中", "red")
        print_colored("提示: Windows上请确保Maven的bin目录在PATH中", "yellow")
        return None
    
    maven_cmd = service_config.get("maven_cmd", "spring-boot:run")
    
    print_colored(f"启动 {service_config['name']} (端口: {service_config['port']})...", "cyan")
    print_colored(f"  使用Maven命令: {mvn_command}", "yellow")
    
    # 先尝试编译，确保类文件存在
    print_colored(f"  正在编译项目...", "yellow")
    compile_cmd = [mvn_command, "clean", "compile"]
    if platform.system() == "Windows":
        compile_result = subprocess.run(
            compile_cmd,
            cwd=str(service_path),
            capture_output=True,
            text=True,
            shell=True
        )
    else:
        compile_result = subprocess.run(
            compile_cmd,
            cwd=str(service_path),
            capture_output=True,
            text=True
        )
    
    if compile_result.returncode != 0:
        print_colored(f"  编译失败，请检查错误信息:", "red")
        print_colored(compile_result.stderr, "red")
        return None
    
    print_colored(f"  编译成功，正在启动服务...", "green")
    cmd = [mvn_command, maven_cmd]
    
    # Windows和Linux处理方式不同
    if platform.system() == "Windows":
        # Windows: 使用start命令创建新的cmd窗口
        # 构建完整的命令字符串
        cmd_str = " ".join(f'"{arg}"' if " " in arg else arg for arg in cmd)
        # 使用start命令在新窗口中运行
        full_cmd = f'cmd /c start "{service_config["name"]}" cmd /k "cd /d "{service_path}" && {cmd_str}"'
        process = subprocess.Popen(
            full_cmd,
            cwd=str(service_path),
            shell=True,
        )
    else:
        # Linux/Mac: 后台运行
        process = subprocess.Popen(
            cmd,
            cwd=str(service_path),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    
    return process


def start_frontend_service(service_key: str, service_config: Dict) -> subprocess.Popen:
    """启动前端服务"""
    service_path = PROJECT_ROOT / service_config["path"]
    package_json = service_path / "package.json"
    
    if not package_json.exists():
        print_colored(f"错误: 找不到package.json文件 {package_json}", "red")
        return None
    
    if not check_command("npm"):
        print_colored("错误: 未找到npm命令，请确保Node.js已安装并在PATH中", "red")
        return None
    
    npm_cmd = service_config.get("npm_cmd", "dev")
    cmd = ["npm", "run", npm_cmd]
    
    print_colored(f"启动 {service_config['name']} (端口: {service_config['port']})...", "cyan")
    
    # Windows和Linux处理方式不同
    if platform.system() == "Windows":
        # Windows: 使用start命令创建新的cmd窗口
        # 构建完整的命令字符串
        cmd_str = " ".join(f'"{arg}"' if " " in arg else arg for arg in cmd)
        # 使用start命令在新窗口中运行
        full_cmd = f'cmd /c start "{service_config["name"]}" cmd /k "cd /d "{service_path}" && {cmd_str}"'
        process = subprocess.Popen(
            full_cmd,
            cwd=str(service_path),
            shell=True,
        )
    else:
        # Linux/Mac: 后台运行
        process = subprocess.Popen(
            cmd,
            cwd=str(service_path),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    
    return process


def display_service_menu() -> Dict[str, List[str]]:
    """显示服务选择菜单"""
    print_colored("\n" + "="*60, "cyan")
    print_colored("AIdoctor 项目服务启动菜单", "cyan")
    print_colored("="*60, "cyan")
    
    selected_services = {
        "python": [],
        "java": [],
        "frontend": [],
    }
    
    # Python服务
    print_colored("\n【Python服务】", "yellow")
    python_services = list(SERVICES_CONFIG["python"].keys())
    for i, key in enumerate(python_services, 1):
        config = SERVICES_CONFIG["python"][key]
        print_colored(f"  {i}. {config['name']} ({key}) - 端口: {config['port']}", "white")
    
    # Java服务
    print_colored("\n【Java服务】", "yellow")
    java_services = list(SERVICES_CONFIG["java"].keys())
    for i, key in enumerate(java_services, 1):
        config = SERVICES_CONFIG["java"][key]
        print_colored(f"  {len(python_services) + i}. {config['name']} ({key}) - 端口: {config['port']}", "white")
    
    # 前端服务
    print_colored("\n【前端服务】", "yellow")
    frontend_services = list(SERVICES_CONFIG["frontend"].keys())
    start_idx = len(python_services) + len(java_services)
    for i, key in enumerate(frontend_services, 1):
        config = SERVICES_CONFIG["frontend"][key]
        print_colored(f"  {start_idx + i}. {config['name']} ({key}) - 端口: {config['port']}", "white")
    
    # 快捷选项
    total_services = len(python_services) + len(java_services) + len(frontend_services)
    print_colored(f"\n  {total_services + 1}. 启动所有Python服务", "green")
    print_colored(f"  {total_services + 2}. 启动所有Java服务", "green")
    print_colored(f"  {total_services + 3}. 启动所有前端服务", "green")
    print_colored(f"  {total_services + 4}. 启动所有服务", "green")
    print_colored(f"  0. 退出", "red")
    
    print_colored("\n" + "-"*60, "cyan")
    choice = input("请选择要启动的服务（多个服务用逗号分隔，如: 1,2,3）: ").strip()
    
    if choice == "0":
        return None
    
    # 解析选择
    choices = [c.strip() for c in choice.split(",")]
    total = total_services
    
    for ch in choices:
        try:
            idx = int(ch)
            if idx == total + 1:  # 所有Python服务
                selected_services["python"] = python_services
            elif idx == total + 2:  # 所有Java服务
                selected_services["java"] = java_services
            elif idx == total + 3:  # 所有前端服务
                selected_services["frontend"] = frontend_services
            elif idx == total + 4:  # 所有服务
                selected_services["python"] = python_services
                selected_services["java"] = java_services
                selected_services["frontend"] = frontend_services
            elif 1 <= idx <= len(python_services):
                selected_services["python"].append(python_services[idx - 1])
            elif len(python_services) < idx <= len(python_services) + len(java_services):
                selected_services["java"].append(java_services[idx - len(python_services) - 1])
            elif len(python_services) + len(java_services) < idx <= total:
                selected_services["frontend"].append(frontend_services[idx - len(python_services) - len(java_services) - 1])
        except ValueError:
            print_colored(f"无效的选择: {ch}", "red")
    
    # 去重
    selected_services["python"] = list(set(selected_services["python"]))
    selected_services["java"] = list(set(selected_services["java"]))
    selected_services["frontend"] = list(set(selected_services["frontend"]))
    
    return selected_services


def main():
    """主函数"""
    global SERVICES_CONFIG
    
    print_colored("AIdoctor 项目一键启动脚本", "cyan")
    print_colored("="*60, "cyan")
    
    # 加载服务配置
    SERVICES_CONFIG = load_services_config()
    
    # 显示菜单并选择服务
    selected_services = display_service_menu()
    
    if selected_services is None:
        print_colored("退出启动", "yellow")
        return
    
    # 检查是否有服务被选中
    total_selected = sum(len(v) for v in selected_services.values())
    if total_selected == 0:
        print_colored("未选择任何服务", "yellow")
        return
    
    print_colored(f"\n准备启动 {total_selected} 个服务...", "cyan")
    print_colored("-"*60, "cyan")
    
    # 启动服务
    processes = []
    
    # 启动Python服务（添加延迟以避免conda run并发冲突）
    for i, service_key in enumerate(selected_services["python"]):
        service_config = SERVICES_CONFIG["python"][service_key]
        # 优先使用配置文件中指定的conda_env，如果没有则尝试自动检测
        service_conda_env = service_config.get("conda_env")
        if not service_conda_env:
            # 如果配置文件中没有指定，则尝试自动检测
            service_conda_env = find_conda_env()
        
        # 检查是否需要使用conda run（需要延迟以避免并发冲突）
        python_exe = find_conda_python(service_conda_env) if service_conda_env else None
        if service_conda_env and not python_exe:
            # 需要使用conda run，添加延迟以避免并发冲突
            if i > 0:
                time.sleep(0.5)
        
        process = start_python_service(service_key, service_config, service_conda_env)
        if process:
            processes.append((service_key, service_config["name"], process))
    
    # 启动Java服务
    for service_key in selected_services["java"]:
        service_config = SERVICES_CONFIG["java"][service_key]
        process = start_java_service(service_key, service_config)
        if process:
            processes.append((service_key, service_config["name"], process))
    
    # 启动前端服务
    for service_key in selected_services["frontend"]:
        service_config = SERVICES_CONFIG["frontend"][service_key]
        process = start_frontend_service(service_key, service_config)
        if process:
            processes.append((service_key, service_config["name"], process))
    
    # 显示启动结果
    print_colored("\n" + "="*60, "cyan")
    print_colored("服务启动完成", "green")
    print_colored("="*60, "cyan")
    print_colored(f"\n已启动 {len(processes)} 个服务:", "green")
    for service_key, service_name, process in processes:
        print_colored(f"  ✓ {service_name} ({service_key}) - PID: {process.pid}", "green")
    
    print_colored("\n提示:", "yellow")
    print_colored("  - 在Windows上，每个服务会在新的命令行窗口中运行", "white")
    print_colored("  - 关闭对应的命令行窗口即可停止服务", "white")
    print_colored("  - 按 Ctrl+C 可以退出此脚本（不会停止已启动的服务）", "white")
    
    # 等待用户中断
    try:
        input("\n按 Enter 键退出...")
    except KeyboardInterrupt:
        print_colored("\n\n退出中...", "yellow")


if __name__ == "__main__":
    main()

