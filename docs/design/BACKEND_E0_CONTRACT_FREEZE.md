# CommerceFlow 后端 E0：展示版契约冻结

> 状态：本轮已完成。日期：2026-09-03。本轮只冻结现有 Java API、事实字段和安全边界，不实现认证、真实 Provider、Outbox 或公网部署。

## 1. 本轮目的

电商后端下一阶段按 `E0`～`E4` 逐步推进。E0 先保护当前前端、Admin、H5 和 Showcase 脚本依赖的真实契约，后续重构只能在这层兼容保护下进行。

当前仍然保留未版本化的 `/api/...` 路径；`/api/v1` 是后续目标，不在 E0 中切换。

## 2. 冻结的内容

### API 路径

- `POST /api/auth/demo-login`
- `GET /api/products`、`GET /api/products/{id}`
- `GET/POST/PUT/DELETE /api/cart...`
- `GET/POST /api/orders...`
- `GET /api/orders/{orderNo}/execution-evidence`
- `POST /api/ai/customer-service/ask`
- 已废弃但保留明确错误语义的 `POST /api/ai/product-chat`
- `GET /api/operations/overview`

路径常量集中在 `CommerceApiContract`；Controller 仍提供原来的 HTTP 方法和路由，不改变前端调用方式。

### 事实和响应

- 当前金额合同是 `CNY`。
- 当前订单状态只有 `CREATED`；没有凭空增加 `PAID`、`SHIPPED` 或 `REFUNDED`。
- `OrderSummary` 的订单号、用户、金额、币种、状态、创建时间和明细字段顺序保持不变。
- AI 客服响应继续保留 `traceId`、请求 ID、回答状态、Provider、Evidence、Java-owned business facts、Trace、耗时、fallback 和 warning。
- `FALLBACK_ANSWER` 等现有 AI 状态继续保留。

### 展示版身份边界

- 旧接口的 `userId=1` 默认参数仍为兼容行为，但只代表 `DEMO_USER` Showcase 模式。
- 本轮没有把 query 参数伪装成生产认证。
- 后续 E1 才处理当前用户上下文、资源归属和 `/api/v1/me/...` 目标路径。

## 3. 实现和测试

- 新增 `apps/mall-api/src/main/java/com/commerceflow/mall/api/CommerceApiContract.java`。
- `ApiModels.SUPPORTED_CURRENCY` 改为引用统一合同；各 Controller 的路由和 Showcase 默认用户常量改为引用合同。
- 新增 `CommerceApiContractTest`，通过 Spring MVC 注解反射固定 Controller 路径、HTTP 方法、订单状态、金额合同和关键响应 record shape。
- 没有新增生产依赖；没有引入 Spring Modulith 或 ArchUnit。

验证命令：

```text
cd apps/mall-api
./mvnw.cmd test
```

退出条件：契约测试和现有 Java/H2/Redis 回归全部通过，旧展示页面和订单证据字段不变。

## 4. 下一阶段

`E1` 已在后续轮次中完成：展示版身份从版本化 Controller 的默认 `userId` 依赖中隔离出来，建立了 `CurrentUserPort` 和 `/api/v1/me/...` 边界；当前仍不宣称生产认证。

`E2` 已在后续轮次中完成订单端口、库存预留端口和同库事务 Outbox 写入基础。E0 本轮本身不改变 MySQL schema、Redis 限流、FastAPI 边界或真实 Provider 状态。
