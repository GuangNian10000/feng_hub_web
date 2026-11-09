# 机器人多平台控制系统

基于 **Kotlin Multiplatform (KMP)** 构建的多端机器人控制系统，支持 **Android**、**iOS** 以及 **Web (网页端)**。该项目利用 **Compose Multiplatform (Wasm)** 实现了跨平台的 UI 100% 共享，旨在为机器人提供直观、高效的交互控制界面。

## 🚀 核心功能

*   **实时地图交互**：集成 SLAM 栅格地图展示，支持点云数据渲染、路径规划显示及平滑的缩放位移操作。
*   **机器人运动控制**：
    *   **双摇杆操作**：左摇杆控制平移与方向，右摇杆控制旋转。
    *   **急停系统**：全局一键急停，确保操作安全。
*   **智能交互模块**：
    *   **导览管理**：支持导览点位的增删改查、坐标配置及到达动作设置。
    *   **语音合成 (TTS)**：自定义语料库，支持多种音色选择及动作关联播放。
    *   **智能体 (Agent)**：支持 AI 角色配置，包括系统提示词 (Prompt) 与个性化设置。
    *   **知识库 (RAG)**：本地化问答库管理，提升机器人的交互能力。
*   **动作库管理**：支持姿态动作与手臂动作的触发、收藏与快速调用。

## 🏗 项目架构

项目采用 MVVM 架构，最大化复用业务逻辑：

*   **[/composeApp](./composeApp)**: 核心逻辑共享层。
    *   `commonMain`: 包含跨平台 UI (Compose Multiplatform)、ViewModels、数据模型 (Beans) 以及通用通信协议层。
    *   `androidMain`: Android 平台特有实现及应用入口。
    *   `iosMain`: iOS 平台适配逻辑。
    *   `wasmJsMain`: Web 平台 (Kotlin/Wasm) 适配逻辑、浏览器入库点及 WebSocket 通信层。
*   **[/iosApp](./iosApp)**: iOS 原生工程入口，负责集成共享的 KMP 模块。

## 🛠 技术栈

*   **UI 框架**: Compose Multiplatform (Android / iOS / Web Wasm)
*   **异步处理**: Kotlin Coroutines & Flow
*   **网络通信**: 高性能 TCP / WebSocket 统一客户端 (支持 JSON 指令序列化与自动重连)
*   **HTTP 引擎**: Ktor Client 3.0 (Android/iOS 使用 CIO 引擎，Web 使用浏览器标准 Fetch/Js 引擎)
*   **本地存储**: Multiplatform Settings (移动端持久化 / Web 端 LocalStorage)
*   **依赖注入/管理**: Koin 4.0 (支持 Compose Multiplatform 及 ViewModel)

## 📦 编译与运行

### 环境要求
*   Android Studio Jellyfish 或更高版本
*   Xcode 15+ (仅限 iOS 开发)
*   JDK 21
*   现代浏览器 (Chrome 119+, Safari 18+, Edge, Firefox 120+ 等支持 WasmGC 的浏览器)

### Web (网页端)
开发阶段可一键启动热重载开发服务器：
```shell
./gradlew :composeApp:wasmJsBrowserRun
```
构建生产环境静态资源（输出位于 `composeApp/build/dist/wasmJs/productionExecutable`）：
```shell
./gradlew :composeApp:wasmJsBrowserDistribution
```

### Android 端
你可以直接在 Android Studio 中运行 `composeApp` 配置，或者使用命令行：

```shell
./gradlew :composeApp:assembleDebug
```

### iOS 端
1.  在 Android Studio 中运行 `iosApp` 模拟器配置。
2.  或者进入 `iosApp` 目录使用 Xcode 打开 `.xcworkspace` 文件进行编译运行。

## 📝 开发规范
*   **UI 风格**: 遵循沉浸式深色设计模式，二级面板标题统一使用 `《 标题` 风格，并支持点击标题快速关闭/返回。
*   **数据协议**: 所有机器人交互通过 TCP / WebSocket 发送标准 JSON 报文，确保数据字段与 `InspectionPointBean` 等模型同步。

---
© 2024 Guangnian Project. Powered by Kotlin Multiplatform.
