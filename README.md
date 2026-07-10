# NekoMaid Modern

NekoMaid Modern 是面向现代 Paper 服务端的网页管理插件。本分支在原版 NekoMaid 基础上移除了已停更的 Uniporter 前置，将管理前端、Socket.IO 后端和文件传输服务整合到单个插件中，并加入 AI 日志诊断功能。

> 当前项目仍处于翻新测试阶段。部署到正式服务器前，请先备份配置、世界与插件目录。

## 主要功能

- 独立运行，无需安装 Uniporter
- 内置原版 NekoMaid React 管理界面
- 查看服务器状态、TPS、内存、玩家和世界信息
- 网页终端、文件管理、插件管理和计划任务
- 玩家背包、方块、实体与服务器配置管理
- OpenAI 兼容 AI 日志分析，可读取近期控制台日志并提供修复建议
- Token 身份验证和临时管理链接
- 支持本地部署，也可通过反向代理安全访问

## 兼容性

- Java：运行环境支持 Java 25，插件字节码目标为 Java 21
- 服务端：面向现代 Paper API 构建
- 已测试：Paper 26.2-53 / Java 25.0.3
- 计划兼容：Paper 26.1、26.1.2、26.2 与 Minecraft 1.21.x

Spigot 及其他 Paper 分支尚未完整验证。高版本服务端会持续移除旧 API，部分高级管理功能可能需要进一步适配。

## 安装

1. 从 Releases 下载 NekoMaid JAR，或在本地执行 `gradlew build`。
2. 将 JAR 放入服务端的 `plugins` 目录。
3. 启动服务器，等待生成 `plugins/NekoMaid/config.yml`。
4. 在控制台执行 `/nekomaid` 或 `/nm` 获取管理地址。
5. 默认本地地址为 `http://127.0.0.1:8088/`，管理链接会自动附带 Token。

插件首次启动会生成随机 Token。请勿公开完整管理链接，也不要把真实 Token 或 AI API Key 提交到 GitHub。

## 常用命令

- `/nekomaid`：获取管理页面地址
- `/nekomaid temp`：生成 15 分钟有效的临时地址
- `/nekomaid reload`：重新加载配置
- `/nekomaid diagnostic`：检查管理地址配置
- `/nekomaid ai <问题>`：使用 AI 分析近期日志
- `/nekomaid block`：在网页中编辑所指向的方块
- `/nekomaid entity`：在网页中编辑所指向的实体
- `/nekomaid invalidate`：使所有临时管理地址失效

## 基础配置

```yaml
token: ~

hostname: 127.0.0.1
customAddress: ''

web:
  enabled: true
  host: 127.0.0.1
  port: 8088
  public-url: ''
  https: false
  local-frontend: true

debug: false
```

如果需要从其他设备访问，推荐使用带 HTTPS 的反向代理，并通过防火墙限制来源。不要直接把未加密的管理端口暴露到公网。

## AI 日志诊断

AI 功能支持 OpenAI 兼容的 `/chat/completions` 接口，例如 OpenAI、兼容网关或本地模型服务。

```yaml
ai:
  enabled: true
  base-url: https://api.openai.com/v1
  api-key: ''
  model: gpt-4.1-mini
  timeout-seconds: 60
  max-log-lines: 200
```

启用后可在管理页面打开“AI 助手”，或执行 `/nekomaid ai <问题>`。发送请求时，近期服务器日志会提交给所配置的模型服务，请根据日志敏感程度选择可信的服务端点。

## 构建

前端使用 Node.js 与 pnpm，插件使用 Gradle：

```powershell
pnpm install
pnpm run build
.\gradlew.bat build
```

构建产物位于 `build/libs/NekoMaid-1.0.0.jar`。仓库同时保留已构建前端资源，便于 Gradle 直接将管理页面打包进 JAR。

## 权限

- `neko.maid.use`：允许玩家使用 `/nekomaid` 命令

## 截图

![Dashboard](screenshots/0.png)
![Players](screenshots/1.png)
![Terminal](screenshots/2.png)

## 致谢

- 原项目：[neko-craft/NekoMaid](https://github.com/neko-craft/NekoMaid)
- 原作者：Shirasawa
- AI 功能设计参考：[Kilacraft-AI](https://github.com/axy-yxa/Kilacraft-AI)

## 许可证

本项目沿用 [GPL-3.0 License](./LICENSE)。
