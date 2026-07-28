# P5 当前基线

审计日期：2026-07-28。分支起点：`bd8706ad306a2f2730352639b210213a76b3e055`。

## 已有真实链路

- 唯一目标入口是 `POST /api/ai/customer-service/ask`，由 `AiController` 转交 `AiService`。
- 请求体含 `userId`、`productId`、`skuId`、`question`、`clientRequestId`。`AiService` 自行校验正数、问题长度和 `clientRequestId` 格式；`userId` 是 Showcase 请求体字段，并非认证主体，不能视为可信生产身份。
- 校验后才读取 Product、SKU、Inventory，构造 `businessFacts`，再调用 Python。Java 保存 `ai_trace`，并返回 7 项 Evidence、6 个 Trace 步骤和安全 fallback。
- 当前全局异常体为 `{ timestamp?, code, message }`；没有 429 专用异常、`Retry-After` 或限流响应头。
- Vue 已用 `isSending` 禁用发送、SKU 切换和快捷问题，避免单页双击重复提交；网络失败会保留问题并显示通用“重试请求”。它不解析 429、剩余额度或重置时间。

## 当前依赖与运行能力

- Spring Boot 为 `3.4.5`，Java 17。`pom.xml` 不含 `spring-boot-starter-data-redis`、Lettuce 显式依赖或 Testcontainers。
- `docker-compose.yml` 只有 `mysql:8.4`，已有 healthcheck；没有 Redis 服务、Redis 端口或 Redis 环境变量。
- `.env.example` 仅有 MySQL、端口及 AI Provider 占位配置。脚本只启动 MySQL；CI 运行 Java、Python、Vue 和 UniApp 构建。
- Java 使用 Spring Boot Test、MockMvc、Mockito 和 H2 测试；没有真实 Redis 集成测试能力。Vue 使用 Vitest。

## 结论与边界

P5A 是设计锁定，不运行 Redis、不添加依赖、不改任何业务代码或配置。P5B 才可增加 Redis、限流组件、HTTP 契约、测试和前端交互。Redis 在 P5 仅存短期计数器；不会存储或取代订单、库存、购物车、会话、产品事实、AI 回答、Evidence、Trace 或 MySQL 事务。
