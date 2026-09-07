# API Contract

本文记录当前仓库实际实现的接口边界。`/api/v1/me` 是消费者范围，`/api/v1/operator` 是管理范围；身份和授权由 Java 后端决定，前端传入的 `userId`、`operatorId`、`isAdmin` 或角色字段都不是授权凭证。

## Runtime identity boundary

基础 `application.yml` 的安全默认值是 `mode=LOCAL`、`authentication-mode=NONE`、`operator-authentication-mode=NONE`、两个 ID 为 `0`、旧接口关闭。只有本地/演示启动显式配置下列 Demo 组合时，Showcase 适配器才会提供身份：

```text
SHOWCASE_MODE=DEMO
SHOWCASE_AUTHENTICATION_MODE=DEMO_USER
SHOWCASE_DEMO_USER_ID=1
SHOWCASE_OPERATOR_AUTHENTICATION_MODE=DEMO_OPERATOR
SHOWCASE_DEMO_OPERATOR_ID=9001
COMMERCEFLOW_LEGACY_API_ENABLED=true   # 仅 LOCAL/DEMO 有效
```

这不是密码、Session、JWT、OIDC/OAuth2 或生产 RBAC。`staging` 和 `production` profile 使用 external 身份模式、ID 为 `0`、关闭旧接口；在真实身份适配器接入前，受保护接口应返回 `401`（或明确的 `403`），而不是自动使用 Demo 用户。

## Catalog

公开目录读接口不携带用户身份：

- `GET /api/products`
- `GET /api/products/{productId}`

## Consumer API: `/api/v1/me`

消费者接口从 `CurrentUserPort` 派生用户范围，不接收 `userId` query/body 参数作为作用域：

- `GET /api/v1/me`
- `GET /api/v1/me/cart`
- `POST /api/v1/me/cart/items` with `{"skuId":10001,"quantity":1}`
- `PUT /api/v1/me/cart/items/{itemId}` with `{"quantity":2}`
- `DELETE /api/v1/me/cart/items/{itemId}`
- `GET /api/v1/me/orders`
- `POST /api/v1/me/orders` with `Idempotency-Key`
- `GET /api/v1/me/orders/{orderNo}`
- `GET /api/v1/me/orders/{orderNo}/execution-evidence`
- `POST /api/v1/me/ai/customer-service/ask`

AI 请求体只包含 `productId`、`skuId`、`question` 和 `clientRequestId`：

```json
{"productId":101,"skuId":10004,"question":"当前库存还有多少？","clientRequestId":"mobile-unique-id"}
```

Java 在调用既有 `AiService` 前把 `CurrentUserPort` 的用户 ID 放入内部请求，继续返回 Java-owned Evidence、Trace 和限流响应头。订单仍要求 `Idempotency-Key`，金额固定为 CNY，状态范围为 `CREATED`；用户只能读取自己的订单和执行证据，知道其他订单号也不能越过归属检查。

## Operator API: `/api/v1/operator`

管理端跨用户读取使用独立 `OperatorScope` 和 `OperatorAuthorizationPolicy`，不复用消费者身份，也不读取前端角色字段：

- `GET /api/v1/operator/orders`
- `GET /api/v1/operator/orders/{orderNo}`
- `GET /api/v1/operator/orders/{orderNo}/execution-evidence`
- `GET /api/v1/operator/operations/overview`
- `POST /api/v1/operator/ai/customer-service/ask`

管理端 AI 请求体同样不包含 `userId`；Java 使用已授权 Operator 的内部 ID 作为限流和 Trace 的作用域。当前 `ShowcaseOperatorAdapter` 只在显式 `DEMO_OPERATOR` 的 LOCAL/DEMO 模式提供演示 Operator；消费者 Demo 身份不能自动获得跨用户权限。

## Legacy compatibility API: `/api`

旧接口保留给本地/演示迁移期，必须同时满足 `COMMERCEFLOW_LEGACY_API_ENABLED=true` 和 `SHOWCASE_MODE` 为 `LOCAL`/`DEMO`。基础、TEST、STAGING、PRODUCTION 或未知模式不会因为布尔开关而重新打开它们：

- `POST /api/auth/demo-login`
- `GET/POST/PUT/DELETE /api/cart...`（需要显式 `userId`，没有默认值）
- `GET/POST /api/orders...`（写入/列表需要显式 `userId`）
- `GET /api/orders/{orderNo}` 与 `GET /api/orders/{orderNo}/execution-evidence`（兼容读取还需要 Operator 授权）
- `POST /api/ai/customer-service/ask`（旧请求体可含 `userId`，仅兼容用途）
- `POST /api/ai/product-chat` 已废弃，返回 `LEGACY_ENDPOINT_DEPRECATED`
- `GET /api/operations/overview` 是兼容路由，需旧接口开关和 Operator 授权

旧接口关闭时返回 `404 LEGACY_ENDPOINT_DISABLED`；缺少旧接口所需 query 参数返回 `400 VALIDATION_ERROR`。旧接口不应被前端新代码继续使用，也不应被当成公网权限边界。

## Error contract

- `401 UNAUTHENTICATED`：消费者没有可信用户范围；`OPERATOR_UNAUTHENTICATED`：管理端没有可信 Operator 范围。
- `403 OPERATOR_FORBIDDEN`：当前身份不是允许的 Operator/Admin，不能访问跨用户管理数据。
- `404 ORDER_NOT_FOUND`：消费者读取其他用户订单时按资源归属返回不可见，不泄露订单存在性。
- `404 LEGACY_ENDPOINT_DISABLED`：兼容接口在当前运行模式被关闭。
- `409`：幂等处理中或同一个 key 对应了不同请求体。
- `429 AI_RATE_LIMIT_EXCEEDED`：限流拒绝，包含 `Retry-After` 和真实限流元数据。

未授权、禁止、Not Found 和后端不可用状态由两个前端统一显示；真实后端失败不会切换为本地伪造数据。

## AI provider boundary

当前本地默认 Provider 是确定性 `commerceflow-mock`。Java 的 OpenAI-compatible/DeepSeek 适配器仅做本地协议测试，真实模型、真实 API Key、计费和模型质量不在本轮验收范围；Provider 不能访问 MySQL、修改订单库存或回调 Java。
