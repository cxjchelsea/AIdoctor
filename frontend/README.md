# 智能诊断系统 - 前端

智能诊断系统的前端应用，基于 React + TypeScript + Vite + Ant Design 构建。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI组件库**: Ant Design 5
- **状态管理**: Zustand
- **路由**: React Router v6
- **HTTP客户端**: Axios

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 项目结构

```
frontend/
├── public/              # 静态资源
├── src/
│   ├── pages/          # 页面组件
│   │   ├── DiagnosisPage.tsx          # 诊断对话页面
│   │   ├── DiagnosisResultPage.tsx    # 诊断结果页面
│   │   ├── ExaminationPage.tsx        # 检查页面
│   │   └── DiagnosisHistoryPage.tsx   # 诊断历史页面
│   ├── components/     # 公共组件
│   ├── services/       # API服务
│   ├── stores/         # 状态管理（Zustand）
│   ├── types/          # TypeScript类型定义
│   ├── utils/          # 工具函数
│   ├── App.tsx         # 根组件
│   └── main.tsx        # 入口文件
├── package.json
├── tsconfig.json       # TypeScript配置
└── vite.config.ts      # Vite配置
```

## 开发指南

### API配置

API基础路径配置在 `vite.config.ts` 中的 `server.proxy` 部分。

默认代理配置：
- 开发环境：`/api` -> `http://localhost:8084`

### 环境变量

可以创建 `.env` 文件配置环境变量：

```env
VITE_API_BASE_URL=http://localhost:8084/api/v1
```

## 功能模块

### 1. 诊断对话页面
- 对话式问诊界面
- 信息收集和进度显示
- 追问问题展示

### 2. 诊断结果页面
- 疾病可能性展示
- 建议检查列表
- 就医建议

### 3. 检查页面
- 检查报告上传
- OCR识别结果展示
- 检查方案设计

### 4. 诊断历史页面
- 诊断记录列表
- 历史记录查看

## 待实现功能

查看 [TODO_待实现功能.md](./TODO_待实现功能.md) 了解待实现的功能清单。

当前待实现功能包括：
- 🔄 工作态切换功能（FEATURE-001）：支持临床诊疗态用户切换到健康管理态

## 开发注意事项

1. 代码规范：使用 ESLint 进行代码检查
2. 类型安全：充分利用 TypeScript 的类型系统
3. 组件复用：提取公共组件到 `components` 目录
4. API调用：统一使用 `services` 目录下的 API 服务
5. 功能管理：新功能或待实现功能记录在 `TODO_待实现功能.md` 中

## 许可证

MIT License


