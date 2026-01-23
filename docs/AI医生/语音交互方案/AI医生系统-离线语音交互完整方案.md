# AI医生系统 - 离线语音交互完整方案

> **文档定位**：本文档是AI医生系统的**离线语音交互完整方案**，包含语音唤醒、语音转文字（STT）、文字转语音（TTS）三个核心模块的技术选型、架构设计、实现细节、性能评估和优化建议。  
> **核心目标**：实现完全离线的语音交互功能，满足准确性和实时性要求，在 Electron 桌面应用中正常使用。  
> **参考文档**：
> - 《Electron-Whisper离线语音识别集成方案.md》- 基础 STT 方案
> - 《AI医生系统-技术架构设计.md》- 系统技术架构
> - 《AI医生系统-系统功能设计.md》- 系统功能设计

---

## 一、方案概述

### 1.1 需求目标

**核心功能**：
- ✅ **语音唤醒**：支持离线唤醒词检测，从静默状态激活语音交互
- ✅ **语音转文字（STT）**：使用 OpenAI Whisper 进行离线语音识别
- ✅ **文字转语音（TTS）**：支持离线文字转语音合成
- ✅ **完全离线运行**：无需网络连接，保护用户隐私
- ✅ **Electron 桌面应用**：在 Electron 框架中正常使用
- ✅ **实时性要求**：低延迟响应，流畅交互体验
- ✅ **准确性要求**：高准确率，特别是医学术语识别

**关键性能指标**：
- **唤醒词检测延迟**：≤ 200ms
- **STT 首词延迟**：≤ 500-800ms（良好硬件）
- **TTS 生成延迟**：≤ 300ms（短句）
- **STT 准确率**：≥ 90-95%（清晰环境），≥ 85-90%（医学术语）
- **唤醒词误触发率**：≤ 1次/小时

### 1.2 技术选型总结

| 模块 | 推荐技术 | 替代方案 | 选择理由 |
|------|---------|---------|---------|
| **语音唤醒** | **openWakeWord**（完全开源免费） | Silero VAD、Mycroft Precise | 完全离线、无需 API key、开源免费 |
| **STT（语音转文字）** | **whisper-node-addon**（基于 whisper.cpp） | Vosk、@xenova/transformers | 离线部署、有成熟的 Node.js 绑定、性能优秀 |
| **TTS（文字转语音）** | **Kokoro TTS**（ONNX 格式） | Piper TTS、Coqui TTS、eSpeakNG | 离线支持、自然度高、有 Node.js 支持 |

**⚠️ 重要说明**：
- **Porcupine** 虽然功能强大，但需要 Picovoice 的 API key（即使是免费版也需要注册），不符合"完全离线"要求，已从推荐方案中移除
- **whisper.cpp** 本身可行，但需要选择合适的 Node.js 绑定库（推荐 `whisper-node-addon`）
- **Piper TTS** 需要命令行工具或 Python 集成，在 Electron 中需要额外封装

**⚠️ 架构限制说明**：
- **whisper-node-addon** 是成熟的原生 Node.js 模块，可直接在 Electron 主进程中使用（无需 Python 子进程）
- **唤醒词检测** 和 **TTS** 目前**没有**类似 `whisper-node-addon` 这样成熟的原生 Node.js 模块：
  - **唤醒词**：openWakeWord、Mycroft Precise 主要是 Python 库，需要通过 Python 子进程调用
  - **TTS**：Kokoro TTS、Piper TTS 也主要是 Python 实现，需要通过 Python 子进程调用
  - 因此，这两个功能需要在 Electron **主进程**中通过**子进程**调用 Python 脚本实现
- **替代方案**：
  - 可以使用 ONNX Runtime Node.js 直接加载 ONNX 模型（更复杂，但无需 Python）
  - 可以使用 WASM 方案在渲染进程实现（性能和质量有限）

---

## 二、系统架构设计

### 2.1 完整架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                    Electron 应用架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────┐      IPC      ┌──────────────────┐ │
│  │   渲染进程 (React)         │◄─────────────►│   主进程         │ │
│  │                           │               │                  │ │
│  │  - UI 界面                │               │ ┌──────────────┐ │ │
│  │  - 用户交互                │               │ │ 唤醒词检测    │ │ │
│  │  - 音频播放                │               │ │ (Porcupine)  │ │ │
│  │  - 状态显示                │               │ └──────────────┘ │ │
│  └──────────────────────────┘               │        │         │ │
│           ▲                                  │        ▼         │ │
│           │                                  │ ┌──────────────┐ │ │
│           │                                  │ │ Whisper STT   │ │ │
│           │                                  │ │ (whisper.cpp)│ │ │
│           │                                  │ └──────────────┘ │ │
│           │                                  │        │         │ │
│           │                                  │        ▼         │ │
│           │                                  │ ┌──────────────┐ │ │
│           │                                  │ │ TTS 引擎      │ │ │
│           │                                  │ │ (Piper/Super) │ │ │
│           │                                  │ └──────────────┘ │ │
│           │                                  │                  │ │
│           └─────────── 麦克风音频流 ─────────┘                  │ │
│                                                                   │
│  音频处理流程：                                                   │
│  [麦克风] → [唤醒检测] → [唤醒] → [STT] → [文本] → [业务逻辑]    │
│                                    ↓                              │
│                                  [响应文本] → [TTS] → [音频播放]  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件

#### 2.2.1 渲染进程（React 前端）
- **音频采集模块**：使用 Web Audio API 持续采集麦克风音频流
- **音频播放模块**：播放 TTS 生成的音频
- **UI 交互模块**：显示语音交互状态、识别结果、播放进度
- **IPC 通信模块**：与主进程通信，发送/接收音频数据和控制命令

#### 2.2.2 主进程（Electron Main）

**语音唤醒模块（Wake Word Detection）**：
- **唤醒词引擎**：openWakeWord 唤醒词检测引擎（完全开源免费）
- **音频流监听**：持续监听麦克风音频流（always-listening）
- **唤醒触发**：检测到唤醒词后触发 STT 模块

**STT 模块（Speech-to-Text）**：
- **Whisper 初始化**：加载 whisper.cpp 模型文件
- **音频处理**：接收音频数据，转换为模型所需格式
- **转写服务**：调用 whisper.cpp 进行语音识别
- **流式处理**：支持流式识别，降低延迟

**TTS 模块（Text-to-Speech）**：
- **TTS 引擎初始化**：加载 TTS 模型（Piper 或 Supertonic）
- **文本处理**：文本预处理（标点、停顿等）
- **语音合成**：生成音频数据
- **音频输出**：返回音频数据到渲染进程播放

**IPC 处理模块**：
- 处理渲染进程的请求
- 管理各模块状态和生命周期
- 错误处理和状态通知

#### 2.2.3 模型资源

**唤醒词模型**：
- openWakeWord 唤醒词模型文件（`.onnx` 格式，约 1-2MB）
- 支持自定义唤醒词训练

**STT 模型**：
- Whisper Base 模型（`ggml-base.bin`，约 142MB）
- 或 Whisper Small 模型（`ggml-small.bin`，约 466MB）

**TTS 模型**：
- Kokoro TTS 中文模型（约 50-100MB，ONNX 格式）
- 或 Piper TTS 中文模型（约 50-100MB，需要 Python 环境）

---

## 三、技术实现方案

### 3.1 语音唤醒模块

#### 3.1.1 技术选型：openWakeWord（推荐，完全开源免费）

**选型理由**：
- ✅ **完全离线**：无需网络连接，无需 API key
- ✅ **开源免费**：MIT 许可证，完全开源
- ✅ **低功耗**：always-listening 模式下 CPU 占用低（< 3%）
- ✅ **可自定义**：支持自定义唤醒词训练
- ✅ **跨平台**：基于 Python/ONNX，可在 Node.js 中集成
- ✅ **轻量级**：模型大小约 1-2MB

**技术规格**：
- 模型大小：约 1-2MB
- 内存占用：约 20-50MB
- CPU 占用：< 3%（待机监听），< 10%（检测时）
- 检测延迟：150-300ms
- 采样率：16kHz，单声道，16-bit PCM

#### 3.1.2 替代方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **openWakeWord** | 完全开源免费、无需 API key、可自定义 | 准确率略低于商业方案 | **推荐（完全离线）** |
| **Silero VAD** | 轻量级、语音活动检测、开源 | 需要配合关键词检测 | 适合做 VAD 组件 |
| **Mycroft Precise** | 开源、支持离线训练 | 需要训练自定义模型 | 需要个性化唤醒词 |
| **Porcupine** | 成熟稳定、低功耗、高准确率 | ⚠️ 需要 API key（不符合完全离线） | 不推荐（需要云服务） |

**⚠️ Porcupine 问题说明**：
- Porcupine 虽然是离线的，但**需要 Picovoice 的 API key 才能使用**（即使是免费版也需要注册获取 access key）
- 这不符合"完全离线、无需任何云服务"的要求
- **不推荐用于完全离线方案**

**推荐：openWakeWord**，满足完全离线、开源免费的要求。

#### 3.1.3 实现方案

**方案一：openWakeWord + Node.js 集成**

openWakeWord 基于 Python/ONNX，可以通过以下方式集成到 Electron：

**方式 A：通过子进程调用 Python 服务**（推荐，简单）

```typescript
// electron/main/wake-word/openwakeword-service.ts
import { spawn } from 'child_process';
import { join } from 'path';

class OpenWakeWordService {
  private pythonProcess: any = null;
  private isListening: boolean = false;

  async initialize(modelPath: string): Promise<void> {
    // openWakeWord 需要通过 Python 脚本运行
    // 需要确保环境中安装了 openwakeword 库
    this.modelPath = modelPath;
  }

  startListening(callback: () => void): void {
    if (this.isListening) return;
    
    this.isListening = true;
    
    // 启动 Python 服务监听唤醒词
    const scriptPath = join(__dirname, '../../python/wake-word-detector.py');
    this.pythonProcess = spawn('python', [scriptPath, this.modelPath]);
    
    this.pythonProcess.stdout.on('data', (data: Buffer) => {
      const output = data.toString();
      if (output.includes('WAKE_WORD_DETECTED')) {
        callback();
      }
    });
  }

  stopListening(): void {
    this.isListening = false;
    if (this.pythonProcess) {
      this.pythonProcess.kill();
      this.pythonProcess = null;
    }
  }
}
```

**方式 B：通过 ONNX Runtime Node.js 直接集成**（更复杂，但更高效）

```typescript
// electron/main/wake-word/openwakeword-service.ts
import * as ort from 'onnxruntime-node';

class OpenWakeWordService {
  private session: ort.InferenceSession | null = null;
  private isListening: boolean = false;

  async initialize(modelPath: string): Promise<void> {
    // 加载 ONNX 模型
    this.session = await ort.InferenceSession.create(modelPath);
  }

  async processAudio(audioData: Buffer): Promise<boolean> {
    if (!this.session) return false;
    
    // 预处理音频数据
    const inputTensor = this.preprocessAudio(audioData);
    
    // 运行推理
    const results = await this.session.run({ input: inputTensor });
    
    // 检查是否检测到唤醒词
    const detected = this.parseResults(results);
    return detected;
  }

  private preprocessAudio(audioData: Buffer): ort.Tensor {
    // 将音频转换为模型输入格式
    // openWakeWord 需要 16kHz, mono, 16-bit PCM
    // 转换为适当的 tensor 格式
    return new ort.Tensor('float32', audioData, [1, audioData.length]);
  }
}
```

**推荐：方式 A（Python 子进程）**，实现简单，openWakeWord 原生支持 Python。

**唤醒词配置**：
- **预设唤醒词**：openWakeWord 提供多个预设唤醒词（如 "hey_jarvis"）
- **自定义唤醒词**：可以训练自定义唤醒词，但需要准备训练数据
- **中文唤醒词**：支持中文，可以训练"医生"、"小助手"等

#### 3.1.4 性能优化

**优化策略**：
1. **模型预加载**：应用启动时预加载唤醒词模型
2. **低功耗模式**：待机时使用低采样率（降低 CPU 占用）
3. **智能休眠**：应用后台时暂停监听（用户可配置）
4. **噪声抑制**：集成噪声抑制算法，提高检测准确率

---

### 3.2 STT 模块（语音转文字）

#### 3.2.1 技术选型：whisper.cpp + Base/Small 模型（推荐）

**选型理由**：
- ✅ **完全离线**：无需网络连接
- ✅ **高准确率**：在清晰环境下 WER ≤ 2-6%
- ✅ **支持中文**：原生支持中文和医学术语
- ✅ **跨平台**：支持 Windows/macOS/Linux
- ✅ **模型可选**：支持多种模型大小（Tiny/Small/Base/Medium/Large）

**技术规格**（基于 Base 模型）：
- 模型大小：约 142MB（Base），466MB（Small）
- 内存占用：约 500-800MB（Base），1-1.5GB（Small）
- 处理速度：实时因子（RTF）≈ 0.3-0.6（良好硬件，Base 模型）
- 首词延迟：约 500-800ms（良好硬件），1-2秒（中等硬件）
- 准确率：WER ≤ 2-6%（清晰环境），≤ 10-15%（噪声环境）

#### 3.2.2 模型选择建议

| 模型 | 大小 | 速度 | 准确率 | 适用场景 |
|------|------|------|--------|---------|
| **Tiny** | 39MB | 最快 | 较低 | 演示、低端设备 |
| **Base** | 142MB | 快 | 良好 | **推荐，平衡选择** |
| **Small** | 466MB | 中等 | 更好 | 高端设备、高准确率需求 |
| **Medium** | 1.5GB | 慢 | 优秀 | 高准确率需求，硬件充足 |
| **Large** | 2.9GB | 很慢 | 最高 | 不推荐（体积太大） |

**推荐：Base 模型**，在速度和准确度之间取得良好平衡。

**特殊场景**：
- **医学术语识别要求高**：使用 Small 模型，准确率更高
- **低端设备**：使用 Tiny 模型或量化版本（INT8/FP16）
- **追求极致准确率**：使用 Small 或 Medium 模型

#### 3.2.3 实现方案

**推荐：whisper-node-addon（实际可用的 Node.js 绑定）**

whisper-node-addon 是基于 whisper.cpp 的成熟 Node.js 原生绑定，专门为 Electron 设计。

**安装**：
```bash
npm install whisper-node-addon
```

**实现代码**：

```typescript
// electron/main/stt/whisper-service.ts
import { Whisper } from 'whisper-node-addon';
// 或者使用 smart-whisper（另一个选择）
// import { SmartWhisper } from 'smart-whisper';

class WhisperService {
  private whisper: Whisper | null = null;
  private isInitialized: boolean = false;

  async initialize(modelPath: string): Promise<void> {
    // 加载 Whisper 模型
    // whisper-node-addon 支持预编译的 .node 文件，无需编译
    this.whisper = new Whisper({
      model: modelPath, // 模型文件路径（ggml-base.bin 等）
      language: 'zh',   // 中文
      translate: false, // 不翻译
      // 可选：GPU 加速（如果支持）
      // useGPU: true,
      // gpuDevice: 0,
    });
    
    this.isInitialized = true;
  }

  async transcribe(audioData: Buffer): Promise<string> {
    if (!this.whisper || !this.isInitialized) {
      throw new Error("Whisper not initialized");
    }

    // 转换为模型所需格式（16kHz, mono, 16-bit PCM）
    const processedAudio = this.preprocessAudio(audioData);
    
    // 执行转写
    // whisper-node-addon 的 API
    const result = await this.whisper.transcribe(processedAudio, {
      language: 'zh',
      translate: false,
    });
    
    return result.text;
  }

  // 流式识别（降低延迟）
  async transcribeStream(audioStream: NodeJS.ReadableStream): Promise<AsyncGenerator<string>> {
    // whisper-node-addon 支持流式处理
    // 分块处理音频，逐步返回识别结果
    for await (const chunk of audioStream) {
      const processedChunk = this.preprocessAudio(chunk);
      const result = await this.whisper.transcribe(processedChunk);
      yield result.text;
    }
  }

  private preprocessAudio(audioData: Buffer): Buffer {
    // 音频预处理：
    // 1. 转换采样率到 16kHz
    // 2. 转换为单声道
    // 3. 转换为 16-bit PCM
    // 4. 降噪处理（可选）
    return processedAudio;
  }
}
```

**替代方案：smart-whisper（另一个选择）**

smart-whisper 是另一个基于 whisper.cpp 的 Electron 友好实现：

```typescript
import { SmartWhisper } from 'smart-whisper';

const whisper = new SmartWhisper({
  modelPath: './models/ggml-base.bin',
  language: 'zh',
});

const text = await whisper.transcribe(audioBuffer);
```

**模型下载**：
- whisper.cpp 官方模型：https://huggingface.co/ggerganov/whisper.cpp/tree/main
- 推荐使用 `ggml-base.bin` 或 `ggml-small.bin`（量化版本）

**流式识别优化**：
- **分块处理**：将音频流分块（如 1-2 秒），逐步识别
- **VAD（语音活动检测）**：自动检测语音开始和结束，减少无效处理
- **增量识别**：使用增量识别技术，降低首词延迟

#### 3.2.4 医学术语优化

**优化策略**：

1. **自定义词汇表**：
   - 构建医学术语词汇表（如"胸痛"、"胸闷"、"心绞痛"等）
   - 在转写时提供词汇提示（prompt），提高识别准确率

2. **后处理优化**：
   - 医学术语标准化（如"心梗" → "心肌梗死"）
   - 常见错误纠正（基于医疗领域知识库）

3. **上下文理解**：
   - 结合对话上下文，提高识别准确率
   - 使用医疗领域语言模型进行纠错

#### 3.2.5 性能优化

**优化策略**：

1. **模型量化**：
   - 使用 INT8 或 FP16 量化模型，减小体积和内存占用
   - 量化后性能影响小（准确率下降 < 2%），速度提升 20-30%

2. **GPU 加速**：
   - 如果有 GPU，使用 GPU 加速（CUDA/OpenCL）
   - GPU 加速可提升速度 5-10倍

3. **多线程处理**：
   - 使用多线程并行处理音频块
   - 提高 CPU 利用率

4. **缓存优化**：
   - 模型预加载并保持常驻内存
   - 避免重复加载模型

---

### 3.3 TTS 模块（文字转语音）

#### 3.3.1 技术选型：Kokoro TTS（推荐，完全离线 + Node.js 支持）

**选型理由**：
- ✅ **完全离线**：无需网络连接，无需 API key
- ✅ **自然度高**：语音质量接近商业 TTS 服务
- ✅ **Node.js 支持**：提供 `kokoro-tts-addon` npm 包，直接集成到 Electron
- ✅ **低延迟**：生成延迟 ≤ 300-500ms（短句）
- ✅ **跨平台**：基于 ONNX Runtime，支持 Windows/macOS/Linux
- ✅ **中文支持**：支持中文语音合成
- ✅ **资源占用低**：模型约 50-100MB，内存占用约 200-500MB

**技术规格**（基于 Kokoro-82M 模型）：
- 模型大小：约 50-100MB（中文模型）
- 内存占用：约 200-500MB
- 生成速度：实时因子（RTF）≈ 0.2-0.4（CPU）
- 生成延迟：≤ 300-500ms（短句），≤ 2秒（长句）
- 音质：MOS 评分 ≥ 3.8/5.0（接近人类语音）

#### 3.3.2 替代方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **Kokoro TTS** | 自然度高、有 Node.js 绑定、完全离线 | 模型选择有限 | **推荐（Electron 集成）** |
| **Piper TTS** | 自然度高、开源免费 | ⚠️ 需要 Python 或命令行工具 | 需要额外封装 |
| **Coqui TTS** | 自然度很高、支持多语言 | 资源占用高、Python 依赖 | 高音质需求 |
| **eSpeakNG** | 体积小、速度快、完全离线 | 自然度较低、机械感强 | 低端设备、提示音 |

**⚠️ Piper TTS 问题说明**：
- Piper TTS 本身很好，但主要提供 Python 命令行工具
- 在 Electron 中集成需要额外封装（子进程调用 Python 或自行编译 Node.js 绑定）
- **推荐使用 Kokoro TTS**，因为它有现成的 Node.js 支持

**推荐：Kokoro TTS**，满足完全离线、Electron 友好、自然度要求。

#### 3.3.3 实现方案

**方案一：Kokoro TTS（推荐，有 Node.js 绑定）**

```typescript
// electron/main/tts/kokoro-service.ts
import { KokoroTTS } from 'kokoro-tts-addon';
// 或使用 ONNX Runtime 直接加载模型
// import * as ort from 'onnxruntime-node';

class KokoroTTSService {
  private kokoro: KokoroTTS | null = null;
  private isInitialized: boolean = false;

  async initialize(modelPath: string): Promise<void> {
    // 加载 Kokoro TTS 模型
    // kokoro-tts-addon 提供了 Node.js 绑定
    this.kokoro = new KokoroTTS({
      model: modelPath, // ONNX 模型路径
      speaker: 'default', // 说话人（如果有多个）
    });
    
    this.isInitialized = true;
  }

  async synthesize(text: string): Promise<Buffer> {
    if (!this.kokoro || !this.isInitialized) {
      throw new Error("Kokoro TTS not initialized");
    }

    // 执行语音合成
    const audioData = await this.kokoro.synthesize(text, {
      speed: 1.0, // 语速
      pitch: 1.0, // 音调
    });
    
    // 返回音频数据（WAV 格式）
    return audioData;
  }

  // 流式合成（长文本分块处理）
  async synthesizeStream(text: string): Promise<AsyncGenerator<Buffer>> {
    const chunks = this.splitText(text);
    
    for (const chunk of chunks) {
      const audio = await this.synthesize(chunk);
      yield audio;
    }
  }

  private splitText(text: string): string[] {
    // 按标点符号和长度分块
    const sentences = text.split(/[。！？\n]/);
    const chunks: string[] = [];
    let currentChunk = '';

    for (const sentence of sentences) {
      if ((currentChunk + sentence).length > 100) {
        if (currentChunk) chunks.push(currentChunk);
        currentChunk = sentence;
      } else {
        currentChunk += sentence;
      }
    }
    
    if (currentChunk) chunks.push(currentChunk);
    return chunks;
  }
}
```

**方案二：Piper TTS（通过子进程调用 Python）**

如果坚持使用 Piper TTS，可以通过子进程调用：

```typescript
// electron/main/tts/piper-service.ts
import { spawn } from 'child_process';
import { join } from 'path';

class PiperService {
  private piperPath: string;
  private modelPath: string;
  private isInitialized: boolean = false;

  constructor() {
    // Piper 需要 Python 环境
    // 假设使用 pip install piper-tts 安装
    this.piperPath = 'piper'; // 或 python -m piper_tts
    this.modelPath = join(__dirname, '../../resources/models/piper-zh-CN-medium.onnx');
  }

  async synthesize(text: string): Promise<Buffer> {
    // 调用 Piper 命令行工具生成音频
    return new Promise((resolve, reject) => {
      const piper = spawn(this.piperPath, [
        '--model', this.modelPath,
        '--output-raw'
      ]);

      let audioData = Buffer.alloc(0);

      piper.stdout.on('data', (chunk: Buffer) => {
        audioData = Buffer.concat([audioData, chunk]);
      });

      piper.on('close', (code) => {
        if (code === 0) {
          resolve(audioData);
        } else {
          reject(new Error(`Piper process exited with code ${code}`));
        }
      });

      piper.stdin.write(text);
      piper.stdin.end();
    });
  }

  // 流式合成（长文本分块处理）
  async synthesizeStream(text: string): Promise<AsyncGenerator<Buffer>> {
    // 将长文本分块，逐步生成音频
    const chunks = this.splitText(text);
    
    for (const chunk of chunks) {
      const audio = await this.synthesize(chunk);
      yield audio;
    }
  }

  private splitText(text: string): string[] {
    // 按标点符号和长度分块
    // 每块长度控制在 50-100 字
    const sentences = text.split(/[。！？\n]/);
    const chunks: string[] = [];
    let currentChunk = '';

    for (const sentence of sentences) {
      if ((currentChunk + sentence).length > 100) {
        if (currentChunk) chunks.push(currentChunk);
        currentChunk = sentence;
      } else {
        currentChunk += sentence;
      }
    }
    
    if (currentChunk) chunks.push(currentChunk);
    return chunks;
  }
}
```

**音频格式**：
- 采样率：22.05kHz 或 24kHz
- 声道：单声道（Mono）
- 位深：16-bit PCM
- 格式：WAV 或 PCM

#### 3.3.4 医学术语优化

**优化策略**：

1. **发音词典**：
   - 构建医学术语发音词典（如"心肌梗死"、"心电图"等）
   - 确保医学术语发音准确

2. **停顿优化**：
   - 在医学术语前后添加适当停顿，提高可理解性
   - 使用标点符号控制停顿时间

3. **语速控制**：
   - 医学术语部分降低语速
   - 一般内容正常语速

#### 3.3.5 性能优化

**优化策略**：

1. **音频缓存**：
   - 缓存常用短语的音频（如"好的"、"请稍等"等）
   - 减少重复合成

2. **预生成**：
   - 在生成回复文本时，后台预生成音频
   - 降低响应延迟

3. **并行合成**：
   - 长文本分块并行合成
   - 提高生成速度

---

## 四、完整交互流程

### 4.1 语音交互流程

```
用户说话
    ↓
[麦克风采集音频流]
    ↓
[唤醒词检测（openWakeWord，持续监听）]
    ↓
[检测到唤醒词]
    ↓
[激活 STT 模块]
    ↓
[Whisper 实时识别语音（whisper-node-addon）]
    ↓
[返回识别文本]
    ↓
[业务逻辑处理（AI医生系统）]
    ↓
[生成回复文本]
    ↓
[TTS 合成语音（Kokoro TTS）]
    ↓
[播放语音反馈]
    ↓
[返回待机状态（继续监听唤醒词）]
```

### 4.2 IPC 通信协议

#### 4.2.1 唤醒词模块 IPC

**渲染进程 → 主进程**：
- `wake-word:init` - 初始化唤醒词模块
- `wake-word:start` - 开始监听
- `wake-word:stop` - 停止监听
- `wake-word:set-sensitivity` - 设置敏感度

**主进程 → 渲染进程**：
- `wake-word:detected` - 检测到唤醒词
- `wake-word:ready` - 初始化完成
- `wake-word:error` - 错误通知

#### 4.2.2 STT 模块 IPC

**渲染进程 → 主进程**：
- `stt:init` - 初始化 Whisper，加载模型
- `stt:transcribe` - 发送音频数据进行转写
- `stt:transcribe-stream` - 开始流式转写
- `stt:stop` - 停止转写
- `stt:get-status` - 获取 STT 状态

**主进程 → 渲染进程**：
- `stt:ready` - Whisper 初始化完成
- `stt:result` - 转写结果
- `stt:partial` - 部分转写结果（流式）
- `stt:progress` - 转写进度更新
- `stt:error` - 错误通知

#### 4.2.3 TTS 模块 IPC

**渲染进程 → 主进程**：
- `tts:init` - 初始化 TTS 引擎
- `tts:synthesize` - 合成语音
- `tts:synthesize-stream` - 流式合成
- `tts:get-status` - 获取 TTS 状态

**主进程 → 渲染进程**：
- `tts:ready` - TTS 初始化完成
- `tts:audio` - 音频数据
- `tts:progress` - 合成进度更新
- `tts:error` - 错误通知

---

## 五、性能评估与优化

### 5.1 准确性评估

#### 5.1.1 STT 准确率

**评估指标**：
- **WER（Word Error Rate）**：词错误率，目标 ≤ 5%
- **CER（Character Error Rate）**：字错误率，目标 ≤ 3%（中文）
- **医学术语准确率**：目标 ≥ 90%

**测试场景**：
1. **清晰环境**：安静室内，清晰发音
   - 目标：WER ≤ 2-5%
2. **噪声环境**：背景噪声 40-60dB
   - 目标：WER ≤ 10-15%
3. **医学术语**：常见医学术语测试集
   - 目标：准确率 ≥ 90%
4. **不同口音**：不同地区口音
   - 目标：WER ≤ 8-12%

**优化措施**：
- 医学术语词汇表增强
- 噪声抑制算法
- 后处理纠错（基于医疗知识库）

#### 5.1.2 唤醒词准确率

**评估指标**：
- **检测率（Recall）**：目标 ≥ 97%
- **误触发率（False Alarm Rate）**：目标 ≤ 1次/小时
- **延迟**：目标 ≤ 200ms

**测试场景**：
1. **安静环境**：检测率 ≥ 99%，误触发 ≤ 0.5次/小时
2. **噪声环境**：检测率 ≥ 95%，误触发 ≤ 1次/小时
3. **不同距离**：1-3米，检测率 ≥ 95%

**优化措施**：
- 敏感度调优
- 噪声抑制
- 多唤醒词组合（降低误触发）

#### 5.1.3 TTS 自然度

**评估指标**：
- **MOS（Mean Opinion Score）**：目标 ≥ 4.0/5.0
- **医学术语发音准确率**：目标 ≥ 95%

**测试方法**：
- 主观听感评估（用户评分）
- 客观指标评估（频谱分析等）

**优化措施**：
- 医学术语发音词典
- 语速和停顿优化
- 音色选择（选择更自然的音色）

### 5.2 实时性评估

#### 5.2.1 端到端延迟

**延迟组成**：
- **唤醒词检测延迟**：100-200ms
- **STT 首词延迟**：500-800ms（Base 模型，良好硬件）
- **业务处理延迟**：100-500ms（取决于业务复杂度）
- **TTS 生成延迟**：200-300ms（短句）
- **音频播放延迟**：50-100ms

**总延迟**：
- **完整交互延迟**（唤醒 → 语音反馈）：约 1-2秒（良好硬件）
- **流式交互延迟**（唤醒 → 首词输出）：约 600-1000ms

#### 5.2.2 优化措施

**降低延迟**：

1. **模型优化**：
   - 使用更小的模型（Base 而非 Small）
   - 量化模型（INT8/FP16）
   - GPU 加速（如有）

2. **流式处理**：
   - 流式 STT（分块识别）
   - 流式 TTS（分块合成）
   - 边识别边处理

3. **预加载和缓存**：
   - 模型预加载
   - 常用短语音频缓存
   - 预热处理

4. **并行处理**：
   - 音频采集和识别并行
   - 识别和业务处理并行（部分）

---

## 六、实现步骤

### 6.1 第一阶段：环境搭建与基础模块（2-3周）

**任务清单**：

1. **环境搭建**
   - ✅ 安装 Electron 开发环境
   - ✅ 配置项目结构和依赖管理
   - ✅ 设置 IPC 通信框架

2. **唤醒词模块开发**
   - ✅ 集成 Porcupine SDK
   - ✅ 实现音频流采集和监听
   - ✅ 实现唤醒词检测逻辑
   - ✅ IPC 通信封装

3. **STT 模块开发**
   - ✅ 集成 whisper.cpp 或 Node.js 绑定
   - ✅ 下载和配置 Whisper Base 模型
   - ✅ 实现音频转写功能
   - ✅ 基础 IPC 通信

4. **TTS 模块开发**
   - ✅ 集成 Piper TTS
   - ✅ 下载和配置中文模型
   - ✅ 实现文本转语音功能
   - ✅ 基础 IPC 通信

**验收标准**：
- 唤醒词模块可以检测到预设唤醒词
- STT 模块可以识别简单语音（准确率 ≥ 80%）
- TTS 模块可以合成语音（自然度 ≥ 3.5/5.0）

### 6.2 第二阶段：集成与优化（2-3周）

**任务清单**：

1. **完整流程集成**
   - ✅ 实现完整的语音交互流程
   - ✅ 集成到 AI 医生系统业务逻辑
   - ✅ UI 界面集成

2. **性能优化**
   - ✅ 流式识别优化
   - ✅ 延迟优化
   - ✅ 内存和 CPU 占用优化

3. **医学术语优化**
   - ✅ 构建医学术语词汇表
   - ✅ STT 后处理优化
   - ✅ TTS 发音优化

**验收标准**：
- 完整语音交互流程可用
- 端到端延迟 ≤ 2秒（良好硬件）
- 医学术语识别准确率 ≥ 90%

### 6.3 第三阶段：测试与部署（1-2周）

**任务清单**：

1. **功能测试**
   - ✅ 单元测试
   - ✅ 集成测试
   - ✅ 端到端测试

2. **性能测试**
   - ✅ 准确性测试（WER、医学术语等）
   - ✅ 实时性测试（延迟、吞吐量）
   - ✅ 资源占用测试（内存、CPU）

3. **兼容性测试**
   - ✅ Windows 10/11 测试
   - ✅ macOS 测试（如有）
   - ✅ Linux 测试（如有）

4. **打包部署**
   - ✅ 配置 electron-builder
   - ✅ 模型文件打包
   - ✅ 安装包制作

**验收标准**：
- 所有功能测试通过
- 性能指标达到目标
- 安装包可以正常安装和运行

---

## 七、风险评估与应对

### 7.1 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| **Whisper 延迟过高** | 高 | 中 | 使用更小模型、量化、GPU 加速 |
| **医学术语识别准确率低** | 高 | 中 | 词汇表增强、后处理优化、fine-tuning |
| **唤醒词误触发率高** | 中 | 低 | 敏感度调优、多唤醒词组合 |
| **模型体积过大** | 中 | 低 | 模型压缩、可选下载 |
| **跨平台兼容性问题** | 中 | 中 | 充分测试、平台特定优化 |

### 7.2 性能风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| **低端设备性能不足** | 高 | 中 | 提供多档模型选择、降级策略 |
| **内存占用过高** | 中 | 中 | 模型量化、按需加载 |
| **CPU 占用过高** | 中 | 低 | 优化算法、降低采样率 |

### 7.3 用户体验风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| **延迟体验差** | 高 | 中 | 流式处理、预生成、进度提示 |
| **识别错误影响体验** | 高 | 中 | 提供编辑功能、确认机制 |
| **噪音环境效果差** | 中 | 高 | 噪声抑制、环境自适应 |

---

## 八、性能分析与优化建议

### 8.1 实时性（Latency）分析

#### 8.1.1 方案一的延迟组成

**完整交互延迟分解**（良好硬件，CPU 4核+，8GB+）：

| 环节 | 工具 | 延迟 | 说明 |
|------|------|------|------|
| **唤醒词检测** | openWakeWord（Python子进程） | **150-300ms** | 80ms帧处理 + Python子进程IPC开销 + 模型推理 |
| **STT 首词延迟** | whisper-node-addon（原生Node.js） | **500-800ms** | 模型推理（Base模型，良好硬件） |
| **STT 完整识别** | whisper-node-addon | **1-3秒** | 取决于音频长度（10-30秒语音） |
| **TTS 生成延迟** | Kokoro TTS（Python子进程） | **300-600ms** | Python子进程IPC + 模型推理（短句50字内） |
| **TTS 流式首音** | Kokoro TTS（流式模式） | **200-400ms** | 如果支持流式，首音更快 |
| **音频播放** | Web Audio API | **50-100ms** | 渲染进程播放延迟 |
| **总计（端到端）** | 全流程 | **1.2-2.5秒** | 唤醒→STT→业务逻辑→TTS→播放 |

**⚠️ 延迟瓶颈分析**：

1. **Python子进程开销**（唤醒词 + TTS）：
   - 进程启动/通信延迟：50-150ms
   - 序列化/反序列化开销：10-50ms
   - 相比原生调用，额外增加 **100-200ms** 延迟

2. **STT 推理延迟**（最大瓶颈）：
   - 模型越大越慢：Tiny（300ms）< Base（500-800ms）< Small（1-2秒）
   - CPU vs GPU：GPU 可加速 5-10倍
   - 量化影响：INT8 量化可加速 20-30%，准确率下降 <2%

3. **非流式处理**：
   - 等待完整音频/文本完成才处理，延迟累积

#### 8.1.2 实时性优化建议

**优化策略**（按优先级）：

1. **使用 GPU 加速**（最有效）：
   - Whisper 使用 GPU（CUDA/Vulkan/Metal）可加速 5-10倍
   - STT 延迟从 500-800ms 降至 **100-200ms**
   - **总延迟降至 0.6-1.2秒**

2. **启用流式处理**：
   - STT 流式识别：分块处理，逐步返回结果
   - TTS 流式合成：文本分块，边生成边播放
   - **可降低首词/首音延迟 30-50%**

3. **模型量化**：
   - Whisper Base 模型使用 INT8 量化
   - 速度提升 20-30%，延迟降至 **400-600ms**
   - 准确率下降 <2%，可接受

4. **Python 子进程优化**（唤醒词 + TTS）：
   - 保持 Python 进程常驻（不每次启动）
   - 使用进程池复用
   - 使用共享内存或命名管道（减少序列化开销）
   - **可减少 50-100ms 延迟**

5. **预热和预加载**：
   - 应用启动时预加载模型
   - 避免首次调用延迟

### 8.2 准确性（Accuracy）分析

#### 8.2.1 方案一的准确率评估

| 模块 | 工具 | 准确率/可靠性 | 限制条件 |
|------|------|-------------|---------|
| **唤醒词检测** | openWakeWord | **85-95%**（理想环境）<br>**70-85%**（真实环境） | ⚠️ 噪声环境、远场麦克风准确率下降明显<br>False Reject（漏检）率可能 >10% |
| **STT（语音转文字）** | whisper-node-addon | **95-98%**（清晰环境）<br>**85-92%**（医学术语）<br>**80-90%**（噪声环境） | ✅ 准确性高，Whisper 是当前最佳开源 STT<br>医学术语需要词汇表增强 |
| **TTS（文字转语音）** | Kokoro TTS | **4.0-4.2/5.0** MOS评分<br>医学术语发音准确率 **90-95%** | ✅ 自然度较高，接近商业 TTS<br>语调、情感表达有限 |

**⚠️ 准确性问题**：

1. **唤醒词检测**是最大瓶颈：
   - openWakeWord 在真实环境中准确率不稳定
   - 噪声环境、远场麦克风漏检率高
   - 可能影响整体用户体验

2. **医学术语识别**需要优化：
   - Whisper 默认对医学术语准确率略低
   - 需要词汇表增强和后处理

#### 8.2.2 准确性优化建议

1. **唤醒词优化**：
   - 集成 VAD（语音活动检测）减少误触发
   - 噪声抑制预处理
   - 考虑使用多个唤醒词检测器投票（提高准确率但增加延迟）
   - 环境自适应调优（根据环境调整阈值）

2. **STT 医学术语优化**：
   - 构建医学术语词汇表（prompt engineering）
   - 后处理纠错（基于医疗知识库）
   - 使用更大的模型（Small 而非 Base，但延迟增加）

3. **TTS 发音优化**：
   - 构建医学术语发音词典
   - 语速和停顿优化
   - 医学术语部分降低语速

### 8.3 方案一是否最优？

#### 8.3.1 实时性对比

| 方案 | 总延迟（良好硬件） | 延迟组成 | 评价 |
|------|------------------|---------|------|
| **方案一（当前）** | 1.2-2.5秒 | 唤醒（200ms）+ STT（600ms）+ TTS（400ms）+ 其他 | ⚠️ 中等，Python子进程有开销 |
| **优化方案一**（GPU+流式） | 0.6-1.2秒 | 唤醒（150ms）+ STT（150ms，GPU）+ TTS（200ms，流式） | ✅ 较好，接近实时 |
| **统一Python服务** | 1.0-2.0秒 | HTTP通信（50ms）+ 处理（同方案一） | ⚠️ 略差，HTTP有开销 |
| **全部ONNX Node.js** | 0.8-1.5秒 | 全部原生，无子进程开销 | ✅ 最好，但实现复杂 |

#### 8.3.2 准确性对比

| 方案 | 唤醒词准确率 | STT准确率 | TTS质量 | 评价 |
|------|------------|----------|--------|------|
| **方案一（当前）** | 70-85%（真实环境） | 95-98%（清晰） | 4.0-4.2/5.0 | ⚠️ 唤醒词是瓶颈 |
| **统一方案** | 类似 | 类似 | 类似 | 类似 |
| **商业方案**（Porcupine等） | 95-98% | 98-99% | 4.5-4.8/5.0 | ✅ 最好，但需要API key |

#### 8.3.3 综合评价

**方案一在以下情况下是最优或接近最优**：

✅ **优点**：
- 完全离线，无需API key
- 各模块可选择最优工具（Whisper准确率高）
- 模块化设计，便于调优和维护
- 在良好硬件下可达到可接受的延迟（优化后 0.6-1.2秒）

⚠️ **缺点**：
- Python子进程带来额外延迟（100-200ms）
- 唤醒词检测在真实环境中准确率不稳定
- 需要GPU加速才能达到最佳性能

**建议**：
- **如果硬件允许（有GPU或强CPU）**：方案一 + GPU加速 + 流式处理 = **推荐**
- **如果硬件受限**：考虑更小的模型或统一轻量方案
- **如果追求极致准确性**：考虑商业方案（需要API key）或更大的模型

### 8.4 优化后的方案一（推荐配置）

**推荐配置**（最佳实时性和准确性平衡）：

1. **硬件要求**：
   - CPU：4核+，或 GPU（CUDA/Vulkan/Metal）
   - 内存：8GB+（Base模型），12GB+（Small模型）

2. **模型选择**：
   - 唤醒词：openWakeWord（量化模型）
   - STT：Whisper Base（INT8量化）或 Small（如果有GPU）
   - TTS：Kokoro TTS（支持流式）

3. **优化措施**：
   - ✅ GPU加速（Whisper）
   - ✅ 流式处理（STT + TTS）
   - ✅ Python进程常驻（减少启动开销）
   - ✅ 模型预加载
   - ✅ 医学术语词汇表增强

**预期性能**（优化后）：
- 端到端延迟：**0.6-1.2秒**（GPU）或 **1.0-1.8秒**（CPU）
- 唤醒词准确率：**85-90%**（良好环境）
- STT准确率：**95-98%**（清晰环境），**90-93%**（医学术语）
- TTS质量：**4.0-4.2/5.0** MOS评分

## 九、总结与建议

### 9.1 技术可行性结论

**✅ 完全可行**：
- 语音唤醒、STT、TTS 三个模块都有成熟的离线解决方案
- 在 Electron 环境中可以正常集成和使用
- 满足准确性和实时性要求（在合适硬件配置下）

**关键成功因素**：
1. **硬件配置**：建议 CPU 4核+，内存 8GB+，**有 GPU 更佳**
2. **模型选择**：Base 模型（平衡），Small 模型（高准确率，需GPU）
3. **优化措施**：**GPU加速**、**流式处理**、模型量化、**Python进程常驻**
4. **医学术语**：词汇表增强、后处理优化

### 8.2 推荐方案

**核心组件**：
- **唤醒词**：openWakeWord（完全开源免费，无需 API key）
- **STT**：whisper-node-addon + Base 模型（或 Small 模型）
- **TTS**：Kokoro TTS 中文模型（或 Piper TTS，通过 Python 子进程）

**⚠️ 重要修正说明**：
- **Porcupine** 已从推荐方案中移除，因为需要 Picovoice API key，不符合"完全离线"要求
- **whisper.cpp** 使用 `whisper-node-addon` 或 `smart-whisper` 作为实际的 Node.js 绑定
- **Piper TTS** 需要 Python 环境，推荐使用 **Kokoro TTS** 因为它有现成的 Node.js 支持

**性能目标**（良好硬件）：
- 唤醒词检测延迟：≤ 200-300ms（openWakeWord）
- STT 首词延迟：≤ 800ms（whisper-node-addon + Base）
- TTS 生成延迟：≤ 300-500ms（Kokoro TTS）
- 完整交互延迟：≤ 2秒
- STT 准确率：≥ 90%（清晰），≥ 85%（医学术语）

### 8.3 后续优化方向

1. **模型 fine-tuning**：针对医疗领域 fine-tuning Whisper 模型
2. **个性化唤醒词**：支持用户自定义唤醒词训练
3. **多语言支持**：支持英文等其他语言
4. **情感识别**：识别用户语音中的情感（焦虑、紧张等）
5. **语音情绪合成**：TTS 支持不同情感（安慰、鼓励等）

---

## 附录

### A. 参考资料

**推荐方案**：
- **openWakeWord**：https://github.com/dscripka/openWakeWord（完全开源免费）
- **whisper-node-addon**：https://github.com/Kutalia/whisper-node-addon（Electron 友好）
- **smart-whisper**：https://github.com/JacobLinCool/smart-whisper（另一个选择）
- **Kokoro TTS**：https://github.com/pinguy/kokoro-tts-addon（Node.js 支持）
- **Whisper.cpp**：https://github.com/ggerganov/whisper.cpp（底层库）

**替代方案**：
- **Piper TTS**：https://github.com/rhasspy/piper（需要 Python）
- **Vosk**：https://github.com/alphacep/vosk-api（STT 替代方案）
- **Mycroft Precise**：https://github.com/MycroftAI/mycroft-precise（唤醒词替代）
- **Electron**：https://www.electronjs.org/

### B. 模型下载地址

- **Whisper 模型**：https://huggingface.co/ggerganov/whisper.cpp/tree/main
- **Kokoro TTS 模型**：https://kokorotts-ai.com/kokoro-tts-local
- **openWakeWord 模型**：https://huggingface.co/dscripka/openWakeWord（或训练自定义）

### C. 之前方案的问题总结

**⚠️ 原方案存在的问题**：

1. **Porcupine**：
   - ❌ 需要 Picovoice API key（即使是免费版也需要注册）
   - ❌ 不符合"完全离线"要求
   - ✅ **修正**：改用 openWakeWord（完全开源免费）

2. **whisper.cpp 绑定**：
   - ❌ 文档中提到的 `whisper-addon` 可能不存在或不稳定
   - ✅ **修正**：使用实际存在的 `whisper-node-addon` 或 `smart-whisper`

3. **Piper TTS**：
   - ⚠️ 主要提供 Python 命令行工具，Electron 集成需要额外封装
   - ✅ **修正**：推荐使用 Kokoro TTS（有 Node.js 支持），或提供 Piper 的子进程调用方案

### C. 性能测试脚本

（待补充具体测试脚本和数据集）

