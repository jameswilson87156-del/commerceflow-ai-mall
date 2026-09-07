# CommerceFlow 后端 E1：服务器派生用户作用域

> 状态：已完成消费者侧基础能力；Phase 0-B 已进一步完成 Operator 边界与模式 fail-closed 收口。日期：2026-09-04。当前实现仍是显式的 Showcase Demo 适配器，不是生产认证或 RBAC。

## 1. 本轮目标

E0 已经冻结现有 `/api/...` 路径。E1 在不破坏旧客户端的前提下增加一条可演进的版本化链路：浏览器不再通过 query 参数决定购物车、订单或 AI 请求的 `userId`，Controller 统一从 `CurrentUserPort` 获取当前用户。Phase 0-B 又把跨用户管理读取移到独立的 `/api/v1/operator/...` 与 `OperatorScope`，避免消费者身份提升为管理身份。

本轮的重点是先固定依赖方向和安全语义，而不是把本地 Demo 登录包装成真实登录。未来接入 OIDC、JWT 或公司的身份平台时，只替换 `CurrentUserPort` 的实现，不改变下游资源归属代码。

## 2. 实现结构

```text
/api/v1/me/... Controller
        |
        v
CurrentUserPort
        |
        v
ShowcaseCurrentUserAdapter
        |
        v
ShowcaseRuntimeProperties
  authentication-mode=DEMO_USER
  demo-user-id=1
```

`CurrentUserPort` 是 account 模块对“当前用户是谁”的最小出站边界。E1 的版本化消费者 Controller 只消费 `UserScope.userId()`，不读取任意客户端 `userId` 参数：

- `CurrentUserController`：返回当前 Demo 用户摘要；
- `ScopedCartController`：当前用户购物车的查询和写入；
- `ScopedOrderController`：当前用户订单创建、列表、详情和执行证据；
- `ScopedAiController`：将当前用户 ID 由服务端补入既有 AI 事实构造链路。

Phase 0-B 的 Operator Controller 使用独立 `OperatorScope`：

- `OperatorOrderController`：授权后读取跨用户订单和执行证据；
- `OperatorOperationsOverviewController`：授权后读取运营总览；
- `OperatorAiController`：授权后执行管理端 AI 请求，内部使用 Operator ID，不接收消费者 `userId`。

## 3. 新增接口

### 当前用户

- `GET /api/v1/me`

### 当前用户购物车

- `GET /api/v1/me/cart`
- `POST /api/v1/me/cart/items`
- `PUT /api/v1/me/cart/items/{itemId}`
- `DELETE /api/v1/me/cart/items/{itemId}`

### 当前用户订单

- `POST /api/v1/me/orders`
- `GET /api/v1/me/orders`
- `GET /api/v1/me/orders/{orderNo}`
- `GET /api/v1/me/orders/{orderNo}/execution-evidence`

创建订单继续沿用 E0 冻结的 `Idempotency-Key`、CNY、`CREATED`、订单明细快照和库存事务语义。订单详情和执行证据先按当前用户查询，再返回资源；不因为知道订单号就越过归属检查。

### 当前用户 AI 客服

- `POST /api/v1/me/ai/customer-service/ask`

请求体只包含 `productId`、`skuId`、`question` 和可选 `clientRequestId`，不包含 `userId`。服务端将用户作用域补入现有 `AiService`，继续使用已有 Redis 限流、业务事实、Evidence、Trace 和安全 fallback。

## 4. 明确的 Demo 边界

E1/Phase 0-B 当前仅允许显式的以下本地 Demo 运行组合：

```text
SHOWCASE_AUTHENTICATION_MODE=DEMO_USER
SHOWCASE_DEMO_USER_ID=1
SHOWCASE_MODE=DEMO
SHOWCASE_OPERATOR_AUTHENTICATION_MODE=DEMO_OPERATOR
SHOWCASE_DEMO_OPERATOR_ID=9001
```

`ShowcaseCurrentUserAdapter` 在非 LOCAL/DEMO、非 `DEMO_USER` 或 Demo 用户 ID 非正数时返回 `UNAUTHENTICATED`，由全局异常处理映射为 `401`。`ShowcaseOperatorAdapter` 在没有显式 Operator fixture 时返回 `OPERATOR_UNAUTHENTICATED`，消费者身份误用管理接口时返回 `OPERATOR_FORBIDDEN`。这意味着：

- 没有新增密码存储、Token 签发、Session、OIDC/JWT 校验或角色系统；
- 没有把 `source=SHOWCASE_DEMO` 写成生产登录凭证；
- 旧 `/api/cart?userId=1`、`/api/orders?userId=1` 和旧 AI 请求仍是兼容 Showcase 链路，不能当成公网权限边界；
- `COMMERCEFLOW_LEGACY_API_ENABLED=false` 时，旧 query 参数用户作用域接口和 Demo login 会返回 `LEGACY_ENDPOINT_DISABLED`；staging 模板强制关闭这条兼容链路。
- 基础配置没有默认用户，旧接口开关只在 LOCAL/DEMO 有效；接入真实身份前，不能把 E1 描述为“已完成生产认证”。完整矩阵见 [Phase 0-B 边界设计](PHASE_0B_API_BOUNDARY_AUTHORIZATION.md)。

## 5. 验收

- `ScopedApiContractTest` 固定 `/api/v1/me` 的 Controller 路径、HTTP 方法，并检查这些 Controller 不声明 `userId` 的 `@RequestParam`。
- `LegacyApiPolicyTests` 验证关闭配置后旧 `/api/cart` 不能继续读取客户端传入的用户作用域。
- `CurrentUserScopeTests` 覆盖服务端返回配置用户、安全默认和非 Demo 模式拒绝冒充认证；`Phase0BBoundaryApiTests` 覆盖跨用户隔离、Operator 403、staging 401 和 legacy 404。
- Java 测试的最终实测数量以 [Phase 0-B posture assessment](../evidence/PHASE_0B_POSTURE_ASSESSMENT.md) 为准。
- E0 的旧 `/api` 契约、订单事务、AI 安全、Redis 集成和健康检查回归仍在同一套测试中执行。

## 6. 下一步

E2 已完成订单写入所需的 `InventoryPort`、幂等存储端口和事务 Outbox 基础，并用幂等先占、库存回滚、并发扣库存和 Outbox 写入测试保护边界。E3 已完成 Provider SPI、兼容协议适配和低基数指标基础；真实身份、RBAC、备份恢复和公网发布仍应在相应凭证与 staging 验收完成后单独声明。
