# Phase 0-B：接口边界与身份授权收口

> 范围：`D:/workhome/commerceflow-ai-mall`。状态：代码、前后端契约、回归测试与本地文档已收口；当前仍不是生产认证实现。

## 1. 目标与结论

本阶段只处理接口边界和身份授权语义，不重做 UI，不接入真实模型或真实 IdP。

最终采用两条明确的服务器边界：

```text
/api/v1/me/...       -> CurrentUserPort       -> 当前消费者范围
/api/v1/operator/... -> OperatorScope         -> 跨用户管理范围
```

消费者接口的用户 ID 只由后端 `CurrentUserPort` 提供；管理端跨用户读取不能复用消费者身份，也不能由前端传 `userId`、`operatorId`、`isAdmin` 或角色字段决定。`OperatorAuthorizationPolicy` 是 Operator 订单、运营总览和管理端 AI 的统一授权入口。

## 2. 当前实际接口

### 消费者 H5/mobile

- `GET /api/v1/me`
- `GET/POST/PUT/DELETE /api/v1/me/cart...`
- `GET/POST /api/v1/me/orders...`
- `GET /api/v1/me/orders/{orderNo}/execution-evidence`
- `POST /api/v1/me/ai/customer-service/ask`

订单和 AI 请求的 `userId` 不来自 query/body。消费者读取其他用户的订单号时，服务端按当前用户归属返回 `404 ORDER_NOT_FOUND`，不泄露资源存在性。AI 请求体只允许 `productId`、`skuId`、`question` 和 `clientRequestId`；Java 将当前用户作用域补入内部服务请求。

### Admin/Operator

- `GET /api/v1/operator/orders`
- `GET /api/v1/operator/orders/{orderNo}`
- `GET /api/v1/operator/orders/{orderNo}/execution-evidence`
- `GET /api/v1/operator/operations/overview`
- `POST /api/v1/operator/ai/customer-service/ask`

管理端页面不再提供“用户 ID”输入框，不再以消费者 `/api/orders` 或 `/api/operations/overview` 作为跨用户管理入口。管理端 AI 的限流/Trace 内部作用域使用已授权 Operator ID；请求体不携带 `userId`。

旧的无版本路径仍仅作为迁移兼容：

- `/api/cart`、`/api/orders`、旧 `/api/ai` 只有在显式 LOCAL/DEMO 模式且打开旧接口开关时可用；旧购物车/订单写入和列表接口要求显式 `userId`，没有默认值。
- 旧订单详情、执行证据和运营总览还必须通过 `OperatorAuthorizationPolicy`。
- 关闭时返回 `404 LEGACY_ENDPOINT_DISABLED`；旧接口不应被当成公网授权边界。

## 3. 身份来源和依赖方向

### CurrentUserPort

`CurrentUserPort` 是账户模块对“当前消费者是谁”的最小出站端口。`ScopedCartController`、`ScopedOrderController`、`ScopedAiController` 只依赖此端口，不依赖 HTTP 参数里的用户 ID。当前实现 `ShowcaseCurrentUserAdapter` 仅用于 LOCAL/DEMO 的显式演示身份，并在其他环境返回 `UNAUTHENTICATED`。

### OperatorScope

`OperatorScope` 与 `CurrentUserPort` 分开，避免“登录了消费者就能读全量订单”的权限提升。`ShowcaseOperatorAdapter` 仅在显式 `DEMO_OPERATOR` 的 LOCAL/DEMO 组合中返回 Operator；消费者 Demo 身份不会自动成为管理角色。未来接入真实身份平台时，应替换这两个适配器或分别接入认证主体和角色映射，不改变下游资源归属代码。

### 运行模式

| 模式 | 默认/推荐身份 | Demo user/operator | 旧 `/api` 兼容接口 | 受保护接口无真实身份 |
| --- | --- | --- | --- | --- |
| `LOCAL` | `NONE`；需显式配置才有 Demo | 允许，但必须显式 `DEMO_USER`/`DEMO_OPERATOR`、正 ID 和非空名称 | 只有开关为 true 才允许 | 401 |
| `DEMO` | 显式本地 Showcase fixture | 允许，推荐使用 `.env.example` 的 1/9001 | 允许 | 401 |
| `TEST` | 测试资源文件显式 fixture | 只用于自动化测试，不是运行时默认 | 测试可显式打开 | 401 |
| `STAGING` | `EXTERNAL`，ID 为 0 | 禁止 | 强制关闭 | 401/403 |
| `PRODUCTION` | `EXTERNAL`，ID 为 0 | 禁止 | 强制关闭 | 401/403 |
| 未知模式 | fail closed | 禁止 | 关闭 | 401/404 |

基础配置的关键安全默认值是 `authentication-mode=NONE`、`operator-authentication-mode=NONE`、`demo-user-id=0`、`demo-operator-id=0`、`legacy-api-enabled=false`。`application-demo.yml` 和根 `.env.example` 是显式本地演示入口；测试配置显式声明 fixture，不能反向改变基础默认值。

## 4. 错误和前端状态

后端统一错误语义：

- `401 UNAUTHENTICATED`：没有可信消费者身份；`401 OPERATOR_UNAUTHENTICATED`：没有可信管理身份。
- `403 OPERATOR_FORBIDDEN`：当前身份存在，但没有 Operator/Admin 权限。
- `404 ORDER_NOT_FOUND`：消费者范围内不可见；不返回“其他用户订单存在”。
- `404 LEGACY_ENDPOINT_DISABLED`：兼容路由未在当前模式打开。
- `429 AI_RATE_LIMIT_EXCEEDED`：限流拒绝，保留真实 `Retry-After` 元数据。

移动端 `ApiError` 和管理端 `AdminApiError` 将 401、403、404、5xx/网络失败映射为可读状态。对于后端错误，页面显示 `Backend Unavailable` 或对应授权状态，不把错误转换成本地成功或伪造数据。

## 5. 配置隔离

- `application.yml`：安全基础默认值，不创建身份。
- `application-local.yml`：本地 profile 仍默认无身份，可通过外部环境显式开启。
- `application-demo.yml`：显式 Demo fixture，面向可重复的本地 Showcase。
- `application-test.yml`：TEST 模式无运行时 Demo 身份，测试必须显式设置 fixture；`application-staging.yml`、`application-production.yml`：external 身份、零 Demo ID、关闭旧接口，AI 限流 fail closed。
- `src/test/resources/application.yml`：仅测试用 H2 Demo fixture；这是测试前置条件，不是部署默认值。

根 `.env.example` 的 Demo 参数只为本地展示服务。`deploy/staging/.env.example` 保持非 Demo 身份模式，真实身份适配器尚未实现时，staging 受保护路由应明确拒绝访问。

## 6. 生产化缺口（本阶段未声称完成）

本阶段没有实现：密码注册登录、Session、JWT、OIDC/OAuth2、真实 RBAC/ABAC、租户隔离、审计主体签名、真实模型/API Key 验收、公网部署、DNS、TLS 或支付/物流能力。当前 Demo adapter 只证明依赖方向和 fail-closed 行为，不能作为生产认证替代品。

后续真实身份接入至少需要：认证主体到 `CurrentUserPort` 的可信映射、Operator 角色和租户策略、统一 401/403 contract、审计日志主体、staging IdP 验收、密钥与会话轮换，以及旧接口最终下线计划。

## 7. 自动化验证入口

- Java：`ScopedApiContractTest` 检查 `/api/v1/me` 和 `/api/v1/operator` 路由不声明 `userId` RequestParam；`CurrentUserScopeTests` 和 `ShowcaseRuntimePolicyTests` 检查显式 Demo 和安全默认；`Phase0BBoundaryApiTests` 检查跨用户隔离、Operator 403、staging 401、旧接口 404 和无默认参数。
- Admin：Vitest 检查 Operator URL、无 `userId` AI body、Operator 订单证据页面以及 `Unauthorized`/`Forbidden`/`Backend Unavailable` 页面状态；`vue-tsc` 和 Vite build 通过。
- Mobile：Node tests 检查 AI 请求体不含 `userId` 和 401/403/5xx/网络失败分类；Vite/uni-app build 通过；源码 API wrapper 使用 `/api/v1/me`。
- 完整实测结果记录在 [Phase 0-B posture assessment](../evidence/PHASE_0B_POSTURE_ASSESSMENT.md)。
