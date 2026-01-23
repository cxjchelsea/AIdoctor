# Electron + Whisper 离线语音识别集成方案

## 一、方案概述

### 1.1 需求目标
- ✅ 使用 OpenAI Whisper 进行语音识别
- ✅ 完全离线运行（无需网络连接）
- ✅ 在 Electron 桌面应用中正常使用
- ✅ 支持实时语音转文字功能

### 1.2 技术选型

**推荐方案：whisper.cpp + Node.js 绑定**

**选型理由**：
- whisper.cpp 是纯 C++ 实现的 Whisper 推理引擎，性能优秀
- 有成熟的 Node.js 绑定库（如 `whisper-node`、`whisper-addon`）
- 模型文件相对较小（base 模型约 142MB，small 模型约 466MB）
- 完全离线运行，无依赖外部服务
- 支持跨平台（Windows/macOS/Linux）
- 内存占用相对合理

**模型选择建议**：
- **Tiny** (39MB) - 最快，准确度较低，适合演示
- **Base** (142MB) - 推荐，速度与准确度平衡
- **Small** (466MB) - 更高准确度，但更慢
- **Medium** (1.5GB) - 准确度高，但体积大
- **Large** (2.9GB) - 最高准确度，但体积很大，不推荐用于桌面应用

**推荐使用 Base 模型**，在速度和准确度之间取得良好平衡。

---

## 二、系统架构设计

### 2.1 架构图

```
┌─────────────────────────────────────────────────────┐
│              Electron 应用架构                        │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────────┐      IPC      ┌────────────┐   │
│  │   渲染进程 (React)  │◄────────────►│ 主进程     │   │
│  │                  │               │          │   │
│  │  - UI 界面        │               │ - Whisper │   │
│  │  - 用户交互        │               │   初始化   │   │
│  │  - 音频录制        │               │ - 模型加载 │   │
│  │  - 结果显示        │               │ - 语音转写 │   │
│  └──────────────────┘               └────────────┘   │
│           ▲                                 │        │
│           │                                 │        │
│           │                                 ▼        │
│           │                        ┌────────────┐   │
│           │                        │ whisper.cpp│   │
│           │                        │  + 模型文件  │   │
│           │                        └────────────┘   │
│           │                                 │        │
│           └───────── 麦克风 ─────────────┘        │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 2.2 核心组件

#### 2.2.1 渲染进程（React 前端）
- **音频采集模块**：使用 Web Audio API 或 MediaRecorder 采集音频
- **UI 交互模块**：录音按钮、状态显示、结果展示
- **IPC 通信模块**：与主进程通信，发送音频数据，接收识别结果

#### 2.2.2 主进程（Electron Main）
- **Whisper 初始化模块**：加载模型文件
- **音频处理模块**：接收音频数据，转换为模型所需格式
- **转写服务模块**：调用 whisper.cpp 进行语音识别
- **IPC 处理模块**：处理渲染进程的请求，返回识别结果

#### 2.2.3 模型资源
- **模型文件存储**：存储在应用资源目录中
- **模型管理**：支持模型切换、预加载、缓存

---

## 三、项目结构设计

### 3.1 目录结构

```
frontend/
├── electron/                          # Electron 相关代码
│   ├── main.ts                        # 主进程入口文件
│   ├── preload.ts                     # 预加载脚本（安全通信桥接）
│   ├── whisper/                       # Whisper 服务模块
│   │   ├── whisper-service.ts         # Whisper 服务封装
│   │   ├── audio-processor.ts         # 音频处理工具
│   │   └── model-manager.ts           # 模型管理
│   └── utils/
│       └── ipc-handlers.ts            # IPC 处理器
│
├── src/                               # React 前端代码（现有）
│   ├── components/
│   │   └── VoiceInput/                # 语音输入组件
│   │       ├── VoiceInput.tsx         # 主组件
│   │       ├── VoiceRecorder.tsx      # 录音器组件
│   │       └── VoiceTranscript.tsx    # 转录结果显示
│   ├── hooks/
│   │   └── useWhisper.ts              # Whisper Hook
│   ├── services/
│   │   └── electron-api.ts            # Electron API 封装
│   └── types/
│       └── whisper.d.ts               # TypeScript 类型定义
│
├── resources/                         # 应用资源（打包时包含）
│   └── models/                        # Whisper 模型文件
│       └── ggml-base.bin              # Base 模型（142MB）
│
├── package.json                       # 项目配置
├── electron-builder.yml               # Electron 打包配置
└── tsconfig.json                      # TypeScript 配置
```

---

## 四、技术实现方案

### 4.1 依赖包选择

#### 4.1.1 Whisper Node.js 绑定选项

**选项一：whisper-node**
- GitHub: `ggerganov/whisper.cpp` 的 Node.js 绑定
- 优点：官方支持，更新及时
- 安装：需要构建原生模块

**选项二：whisper-addon**
- NPM 包，基于 whisper.cpp
- 优点：安装简单，跨平台预编译
- 缺点：可能更新不及时

**选项三：@xenova/transformers**
- 纯 JavaScript 实现，基于 WebAssembly
- 优点：无需原生模块，跨平台好
- 缺点：性能略低于原生实现

**推荐：优先尝试 whisper-addon，如果不稳定再考虑 whisper-node 自编译**

#### 4.1.2 其他依赖

```json
{
  "dependencies": {
    "electron": "^28.0.0",
    "whisper-addon": "^1.0.0"  // 或其他 whisper 绑定
  },
  "devDependencies": {
    "electron-builder": "^24.9.1",
    "@types/node": "^20.0.0"
  }
}
```

### 4.2 模型文件获取

#### 4.2.1 下载地址
- **官方仓库**：https://github.com/ggerganov/whisper.cpp
- **模型下载**：https://huggingface.co/ggerganov/whisper.cpp/tree/main
- **CDN 镜像**：可以使用国内镜像加速下载

#### 4.2.2 模型格式
- 使用 `ggml-base.bin` 格式（量化后的模型）
- 支持 FP16 或 FP32 格式
- 推荐使用量化版本，体积更小，性能影响小

#### 4.2.3 模型放置
- 开发环境：放在 `resources/models/` 目录
- 生产环境：通过 electron-builder 打包到应用资源目录
- 路径处理：使用 `app.getAppPath()` 获取应用路径

### 4.3 IPC 通信设计

#### 4.3.1 通信协议

**渲染进程 → 主进程**：
- `whisper:init` - 初始化 Whisper，加载模型
- `whisper:transcribe` - 发送音频数据进行转写
- `whisper:getStatus` - 获取 Whisper 状态
- `whisper:setModel` - 切换模型

**主进程 → 渲染进程**：
- `whisper:ready` - Whisper 初始化完成
- `whisper:progress` - 转写进度更新（如果支持）
- `whisper:error` - 错误通知

#### 4.3.2 安全通信

使用 `contextIsolation` 和 `preload` 脚本确保安全：
- 渲染进程通过 `window.electronAPI` 访问
- 主进程通过 IPC handlers 处理请求
- 避免直接暴露 Node.js API 到渲染进程

### 4.4 音频处理流程

#### 4.4.1 音频采集
1. 使用 `navigator.mediaDevices.getUserMedia()` 获取麦克风权限
2. 使用 `MediaRecorder` 或 `Web Audio API` 录制音频
3. 支持实时流式处理或录制后批量处理

#### 4.4.2 音频格式转换
1. 录制格式：通常是 WebM 或 WAV
2. 转换要求：
   - 采样率：16000 Hz（Whisper 标准）
   - 声道：单声道（Mono）
   - 位深：16 bit PCM
3. 转换工具：使用 `ffmpeg` 或 JavaScript 音频处理库

#### 4.4.3 数据传输
1. 将音频数据转换为 ArrayBuffer 或 Buffer
2. 通过 IPC 发送到主进程
3. 主进程接收后转换为模型输入格式

---

## 五、实现步骤

### 5.1 第一阶段：环境搭建

1. **安装 Electron**
   - 在现有 React 项目中集成 Electron
   - 配置开发环境（dev）和生产环境（build）

2. **安装 Whisper 绑定库**
   - 选择合适的 npm 包
   - 处理可能的编译依赖（如需要）

3. **下载模型文件**
   - 下载 base 模型（ggml-base.bin）
   - 放置到项目资源目录

### 5.2 第二阶段：核心功能开发

1. **Electron 主进程设置**
   - 创建主进程文件
   - 配置 BrowserWindow
   - 设置 preload 脚本

2. **Whisper 服务封装**
   - 实现模型加载逻辑
   - 实现音频转写功能
   - 错误处理和状态管理

3. **IPC 通信实现**
   - 定义通信协议
   - 实现 preload 脚本
   - 实现 IPC handlers

### 5.3 第三阶段：前端集成

1. **语音输入组件开发**
   - 录音功能组件
   - 状态显示组件
   - 结果显示组件

2. **Electron API 封装**
   - 封装 IPC 调用
   - 错误处理
   - TypeScript 类型定义

3. **UI 集成**
   - 集成到现有页面
   - 用户体验优化
   - 加载状态提示

### 5.4 第四阶段：优化与测试

1. **性能优化**
   - 模型预加载策略
   - 内存管理优化
   - 音频处理优化

2. **打包配置**
   - 配置 electron-builder
   - 模型文件打包
   - 跨平台测试

3. **测试验证**
   - 功能测试
   - 性能测试
   - 兼容性测试

---

## 六、关键技术点

### 6.1 模型加载策略

**延迟加载**：
- 应用启动时不立即加载模型
- 首次使用时加载
- 加载时显示进度提示

**预加载**：
- 应用启动后后台加载
- 加载完成后缓存
- 减少首次使用等待时间

**推荐：预加载策略**，在应用启动时后台加载模型。

### 6.2 内存管理

**模型内存**：
- Base 模型加载后约占用 500-800MB 内存
- 使用完毕后不要卸载，保持常驻
- 多个实例共享同一模型

**音频缓存**：
- 及时释放处理完的音频缓冲区
- 限制并发转写任务数量
- 使用流式处理避免内存积累

### 6.3 错误处理

**模型加载错误**：
- 文件不存在
- 文件损坏
- 内存不足

**转写错误**：
- 音频格式不支持
- 音频数据为空
- 转写超时

**权限错误**：
- 麦克风权限被拒绝
- 应用权限不足

### 6.4 性能优化

**音频优化**：
- 使用合适的采样率和格式
- 批量处理多个音频片段
- 压缩音频数据减少传输量

**模型优化**：
- 使用量化模型（INT8/FP16）
- 选择合适的模型大小
- 考虑使用 GPU 加速（如果支持）

**用户体验优化**：
- 显示转写进度
- 支持取消转写
- 提供离线模式提示

---

## 七、打包部署方案

### 7.1 electron-builder 配置

**关键配置项**：
- 包含模型文件到应用包
- 设置应用图标和元信息
- 配置自动更新（可选）
- 处理代码签名（生产环境）

**打包大小估算**：
- Base 应用：约 150-200MB
- Base 模型：约 142MB
- **总计：约 300-350MB**

### 7.2 跨平台支持

**Windows**：
- 支持 Windows 10/11
- 可能需要 Visual C++ 运行时
- 处理长路径问题

**macOS**：
- 支持 macOS 10.15+
- 需要处理代码签名和公证
- 处理权限提示

**Linux**：
- 支持主流发行版
- 可能需要额外依赖
- 处理应用菜单集成

### 7.3 模型文件分发策略

**方案一：内置模型**
- 优点：开箱即用
- 缺点：应用体积大

**方案二：首次启动下载**
- 优点：应用体积小
- 缺点：需要网络，首次使用慢

**方案三：可选下载**
- 优点：灵活性高
- 缺点：实现复杂

**推荐：内置模型**，保证离线可用。
