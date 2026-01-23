# 执行追踪管理系统 - 前端

执行追踪管理系统的前端应用，基于 React + TypeScript + Vite + Ant Design 构建。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI组件库**: Ant Design 5
- **路由**: React Router v6
- **HTTP客户端**: Axios
- **WebSocket**: SockJS + STOMP
- **可视化**: ReactFlow

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3001

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
frontend-admin/
├── public/              # 静态资源
├── src/
│   ├── pages/          # 页面组件
│   │   └── TraceManagementPage.tsx    # 执行追踪管理页面
│   ├── components/     # 公共组件
│   │   └── trace/      # 追踪可视化组件
│   │       ├── DataFlowGraph.tsx       # 数据流转图
│   │       ├── ServiceCallGraph.tsx    # 服务调用图
│   │       ├── ExecutionTimeline.tsx   # 执行时间线
│   │       └── ModuleCallTree.tsx      # 模块调用树
│   ├── services/       # API服务
│   │   └── traceApi.ts # 追踪服务API
│   ├── hooks/          # React Hooks
│   │   └── useTraceWebSocket.ts # WebSocket Hook
│   ├── types/          # TypeScript类型定义
│   │   └── trace.ts    # 追踪相关类型
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
- 开发环境：`/api/v1/trace` -> `http://localhost:8093`
- WebSocket：`/api/v1/trace/ws` -> `ws://localhost:8093`

### 环境变量

可以创建 `.env` 文件配置环境变量：

```env
VITE_API_BASE_URL=http://localhost:8093/api/v1/trace
```

## 功能模块

### 执行追踪管理页面

- CDP ID查询和追踪数据加载
- 实时WebSocket追踪
- 多种可视化视图：
  - **数据流转图**：展示数据在服务间的流转
  - **服务调用图**：展示服务间的调用关系和统计
  - **执行时间线**：按时间顺序展示所有追踪事件
  - **模块调用树**：展示服务内部模块的调用层次
- 统计信息展示

## 与用户前端的关系

- **用户前端** (`frontend`): 提供诊断对话、结果展示等用户功能，运行在端口 3000
- **管理前端** (`frontend-admin`): 提供执行追踪管理等管理功能，运行在端口 3001

两个前端项目相互独立，可以分别开发和部署。

## 开发注意事项

1. 代码规范：使用 ESLint 进行代码检查
2. 类型安全：充分利用 TypeScript 的类型系统
3. 组件复用：提取公共组件到 `components` 目录
4. API调用：统一使用 `services` 目录下的 API 服务

## 许可证

MIT License

