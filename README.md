# webhook-notification

为 Halo 常见事件发送可配置的 Webhook 通知。

## 简介

插件支持在 Halo 插件设置页中配置多个 Webhook 目标，并按事件开关决定是否发送通知。

目前支持的事件：

- 用户登录
- 新设备登录
- 修改密码
- 收到评论
- 评论被回复

目前内置的通知格式：

- `generic-json`：适合自定义服务、自动化平台、Webhook 转发器
- `ntfy-markdown`：适合 `ntfy`，自动附加 `Title`、`Markdown`、`Tags`、`Click`
- `slack-compatible`：适合 Slack Incoming Webhook 或兼容 `text` 字段的平台

每个 Webhook 目标都支持：

- 单独启用或停用
- 配置请求地址
- 配置通知格式
- 通过 JSON 配置可选请求头，例如 `Authorization`

## 开发环境

- Java 21+
- Node.js 18+
- pnpm

## 开发

```bash
# 启用插件
./gradlew haloServer
# 开发前端
cd ui
pnpm install
pnpm dev
```

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 使用说明

1. 安装插件并启用。
2. 打开插件设置，添加一个或多个 Webhook 目标。
3. 为每个目标选择合适的通知格式。
4. 在事件开关中启用需要发送的事件。
5. 如目标服务需要鉴权，可填写 JSON 格式的请求头。

Header 示例：

```json
{
  "Authorization": "Bearer your-token",
  "X-Source": "halo"
}
```

## 许可证

[GPL-3.0](./LICENSE) © ziyoung 
