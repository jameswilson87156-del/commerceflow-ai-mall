# Phase 0-B Posture Assessment：接口边界与身份授权

评估日期：2026-09-04  
评估对象：`D:/workhome/commerceflow-ai-mall`  
评估范围：消费者 `/api/v1/me`、Operator 跨用户读、Operations Overview、运行模式隔离、前后端契约与回归验证。  
评估方法：按照 posture assessment 流程执行资产/入口映射、控制核对、边界探测、配置审计、错误语义核对和证据登记。

## 1. 范围和限制

本报告只评估本仓库。本轮没有读取、修改或验证 `D:/workhome/enterprise-ai-ticket-copilot`。本轮也没有接入真实大模型、生产 API Key、真实 OIDC/JWT/Session、完整 RBAC/ABAC、公共部署、DNS/TLS 或支付/物流。

报告中的 `PASS` 只表示当前代码、测试或配置证据支持的边界；`RESIDUAL` 表示仍需后续真实环境、凭据或外部基础设施验证的风险，不能当作已完成能力。

## 2. 基线入口映射（修改前）

| 入口 | 修改前路径/调用方式 | 身份来源 | 风险判断 |
| --- | --- | --- | --- |
| H5/mobile 购物车 | `/api/cart`、客户端固定 `userId=1` | Demo 约定/请求参数 | 参数可表达任意用户范围，且不是当前用户语义 |
| H5/mobile 订单 | `/api/orders`、订单读取链路携带 `userId=1` | Demo 约定/请求参数 | 消费者资源归属依赖客户端输入 |
| H5/mobile AI 客服 | `/api/ai/customer-service/ask`，请求体含用户范围 | 请求体 `userId` | AI 上下文和限流身份边界可被客户端影响 |
| Admin 订单证据 | `/api/orders/{orderNo}`、`/execution-evidence`，页面提供用户 ID 输入 | 前端参数/旧控制器 | 跨用户管理意图没有单独 Operator API 边界 |
| Admin Operations Overview | `/api/operations/overview` | 无后端 Operator 策略 | 指标聚合曾作为无身份公共入口暴露 |
| Admin AI 工作台 | `/api/ai/customer-service/ask` | 管理页/旧请求体 | 管理读与消费者语义混用 |
| 运行配置 | Demo/Showcase 约定与默认值分散 | 配置默认和前端常量 | 非 Demo 环境可能误使用演示身份或旧接口 |

## 3. 控制验证结果

| 控制编号 | 验证内容 | 实现/证据 | 结果 |
| --- | --- | --- | --- |
| P0B-C01 | 消费者用户范围只来自服务端 | `CurrentUserPort`、`ShowcaseCurrentUserAdapter`、`ScopedCartController`、`ScopedOrderController`、`ScopedAiController` | PASS（Demo adapter 仅限显式 LOCAL/DEMO） |
| P0B-C02 | `/api/v1/me` 新链路不接受客户端 `userId` | `CommerceApiContract`、`ScopedApiContractTest`、移动端 `api/cart.ts`、`api/orders.ts`、`api/ai.ts` | PASS |
| P0B-C03 | 消费者不能读取其他用户订单 | `OrderReadService.detailForUser` 归属条件；`Phase0BBoundaryApiTests` 验证其他用户订单返回 `404 ORDER_NOT_FOUND` | PASS |
| P0B-C04 | 跨用户管理读使用独立边界 | `OperatorScope`、`OperatorAuthorizationPolicy`、`/api/v1/operator/orders...` | PASS（角色提供者仍为 Showcase adapter） |
| P0B-C05 | 普通消费者不能提升为 Operator | `ShowcaseOperatorAdapter` 不从 Demo consumer 自动返回 Operator；边界测试验证 `403 OPERATOR_FORBIDDEN` | PASS |
| P0B-C06 | Operations Overview 不再公共暴露 | 新 `/api/v1/operator/operations/overview` 要求 Operator；旧 `/api/operations/overview` 还要求 legacy policy + Operator | PASS |
| P0B-C07 | Admin AI 使用 Operator 路径且不发 `userId` | `OperatorAiController`、admin `aiCustomerService.ts`、AI 工作台测试 | PASS |
| P0B-C08 | 非 Demo、无可信身份时 fail closed | staging/production 配置无 Demo ID；边界测试验证 `/me` 和 `/operator` 返回 401 | PASS |
| P0B-C09 | legacy 兼容接口不由全局布尔值重开 | `LegacyApiPolicy` 要求显式开关且 `LOCAL/DEMO`；TEST/STAGING/PRODUCTION/unknown 测试 | PASS |
| P0B-C10 | 后端错误不静默回退本地成功数据 | mobile `ApiError`、admin `AdminApiError` 和各页面重试状态；无 Mock fallback 逻辑 | PASS（前端错误状态覆盖） |

## 4. 最终接口面

### Consumer / H5/mobile

- `GET /api/v1/me`
- `GET/POST/PUT/DELETE /api/v1/me/cart...`
- `GET/POST /api/v1/me/orders...`
- `GET /api/v1/me/orders/{orderNo}/execution-evidence`
- `POST /api/v1/me/ai/customer-service/ask`

以上新链路不提交 `userId`；Java 从 `CurrentUserPort` 派生当前用户。消费者范围之外的订单以 `404` 不可见处理。

### Operator / Admin

- `GET /api/v1/operator/orders`
- `GET /api/v1/operator/orders/{orderNo}`
- `GET /api/v1/operator/orders/{orderNo}/execution-evidence`
- `GET /api/v1/operator/operations/overview`
- `POST /api/v1/operator/ai/customer-service/ask`

这些接口统一经过 `OperatorAuthorizationPolicy`。前端 role、`isAdmin`、`operatorId` 或 `userId` 都不是授权凭证。当前 Demo Operator 由显式本地配置产生，不等于生产 RBAC。

### Legacy compatibility

仍保留的旧路径包括 `/api/auth/demo-login`、`/api/cart...`、`/api/orders...`、`/api/ai...` 和 `/api/operations/overview`。只有 `legacy-api-enabled=true` 且运行模式为 `LOCAL` 或 `DEMO` 时才允许进入；关闭时返回 `404 LEGACY_ENDPOINT_DISABLED`。旧购物车/订单写入和列表要求显式 `userId`、没有默认值；旧管理详情、执行证据和运营总览仍需 Operator 策略。旧路径是迁移兼容，不是公网授权边界。

## 5. 运行模式审计

| 模式 | 消费者 Demo 身份 | Operator Demo 身份 | legacy | 无真实身份时保护接口 |
| --- | --- | --- | --- | --- |
| `LOCAL` | 只有显式 `DEMO_USER`、正 ID、非空名称 | 只有显式 `DEMO_OPERATOR`、正 ID、非空名称 | 只有显式开关 | 401 |
| `DEMO`/`SHOWCASE` | 允许显式本地 fixture | 允许显式本地 fixture | 只有显式开关 | 401 |
| `TEST` | `application-test.yml` 默认禁止 Demo；测试可在测试上下文中显式改成 fixture | `application-test.yml` 默认禁止 Demo；测试可在测试上下文中显式改成 fixture | 默认关闭，测试可显式配置 | 默认无可信身份时 401 |
| `STAGING` | 禁止 Demo，使用 `EXTERNAL` 配置 | 禁止 Demo，使用 `EXTERNAL` 配置 | 强制关闭 | 401/403 |
| `PRODUCTION` | 禁止 Demo，使用 `EXTERNAL` 配置 | 禁止 Demo，使用 `EXTERNAL` 配置 | 强制关闭 | 401/403 |
| 未知 | 禁止 | 禁止 | 关闭 | fail closed |

基础配置默认为 `NONE`、两个 ID 为 `0`、legacy 关闭；根 `.env.example` 与 `application-demo.yml` 才是显式本地演示入口。前端区分 `Local Demo Fixture` 与 `Real Backend`，401/403/404/5xx/网络失败显示对应状态，不用 fixture 把真实后端错误伪装成成功。

## 6. 验证证据

### Backend

- `.\mvnw.cmd -f apps/mall-api/pom.xml '-Dtest=Phase0BBoundaryApiTests,ShowcaseRuntimePolicyTests' test`：`8/8`，`BUILD SUCCESS`。
- `.\mvnw.cmd -f apps/mall-api/pom.xml '-Dtest=!AiRateLimitRedisIntegrationTests' test`：`79/79`，`0` failures，`BUILD SUCCESS`。
- 完整命令 `.\mvnw.cmd -f apps/mall-api/pom.xml test`：共 `84` 个测试，`0` failures，`5` errors；5 个均来自 `AiRateLimitRedisIntegrationTests` 初始化时连接 `localhost:6380` 被拒绝。没有发现其他断言失败；Redis 集成部分不能标记为绿。
- `Phase0BBoundaryApiTests` 覆盖当前用户购物车/订单、查询参数覆盖无效、其他用户订单不可见、AI user scope、staging 401、Operator 403、legacy 404 和旧参数缺失 400。
- `ShowcaseRuntimePolicyTests` 覆盖显式 LOCAL/DEMO、TEST、STAGING、PRODUCTION 及未知模式的身份/legacy 开关。

### Frontend

- `apps/mobile-app`: `npm test` -> `36/36`；`npm run build` -> success。构建仅报告既有的 Vite CJS、`.env NODE_ENV` 和动态导入提示。
- `apps/admin-web`: `npm test` -> `46/46`；`npm run build`（`vue-tsc --noEmit && vite build`）-> success。
- 移动端源码静态核对：购物车、订单、AI 封装均使用 `/api/v1/me/...`；AI body 只有四个业务字段；无 `DEMO_USER_ID` 前端常量。
- Admin 源码静态核对：订单、证据、运营总览、AI 均使用 `/api/v1/operator/...`；管理页没有 user ID 输入框；错误状态含 `Unauthorized`、`Forbidden`、`Not Found` 和 `Backend Unavailable`。

### Workspace hygiene

- `git diff --check`：无 whitespace error；PowerShell 文件仅有已有 CRLF normalization warning。
- `git status --short`：工作区仍为 dirty，且包含本轮之前的用户改动；本轮未 reset、checkout、clean、删除、commit 或 push。

## 7. 残余风险和后续动作

| 风险 | 等级 | 当前缓解 | 不能声称的内容 | 后续位置 |
| --- | --- | --- | --- | --- |
| 没有真实消费者身份适配器 | High | 非 Demo 没有身份就 401；Demo 只显式配置 | 已完成真实登录、JWT/OIDC 或生产认证 | `TODO.md` |
| 没有真实 Operator 认证/RBAC/租户策略 | High | 独立 `OperatorScope` + 统一策略；非 Demo fail closed | 已完成生产 RBAC/ABAC 或租户隔离 | `TODO.md` |
| legacy userId 接口仍存在 | Medium | 仅 LOCAL/DEMO 显式兼容；staging/production 强制关闭 | 旧接口已删除或可作为公网边界 | `TODO.md` |
| Redis `localhost:6380` 未运行 | Medium | 单元/非 Redis 回归通过；应用保留 fail-open/fail-closed 策略 | Redis 限流集成已在当前环境验收通过 | `TODO.md` |
| staging/production 未做真实部署验收 | Medium | profile 配置静态 fail-closed | 已上线公网、DNS/TLS、备份恢复或生产稳定性 | `TODO.md` |

## 8. 结论

Phase 0-B 的代码和契约边界已经完成：消费者与 Operator 使用不同的服务器授权语义，Operations Overview 不再是无身份公共接口，Demo 身份和 legacy 接口被限制在显式本地/演示模式，前后端错误不会静默回填 Mock 数据。由于真实身份系统和 Redis 集成环境尚未提供，本报告只将上述范围标记为“Showcase/代码层完成”；生产认证、生产 RBAC、真实 Provider 和公网部署仍保持未完成。
