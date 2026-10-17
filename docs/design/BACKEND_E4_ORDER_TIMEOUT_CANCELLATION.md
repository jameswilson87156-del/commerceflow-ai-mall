# BACKEND_E4 · 订单超时取消（Order Timeout Cancellation）

状态：已实现，本地与 CI 测试全绿（mall-api 117 项测试通过，其中 10 项环境相关跳过与升级前一致）。

## 解决的问题

`CREATED` 订单在买家放弃支付后会永久占用库存。本项目没有支付流转，因此用"超时未完成即取消并回补库存"的规则闭合订单生命周期，同时保持这个仓库一贯的纪律：每一步都有可核对的事务证据。

## 行为

1. `insertOrder` 在创建订单时写入 `cancel_deadline = now + ttl`（默认 1800 秒，`commerceflow.order-timeout.ttl-seconds`；deadline 由应用计算后作为普通参数写入，同一语句在 MySQL 8.4 与 H2(MySQL mode) 上都能执行）。
2. `OrderTimeoutCancellationService` 以固定延迟（默认 5 秒，`scan-interval-ms`）扫描 `status='CREATED' AND cancel_deadline <= now`，批大小默认 50。
3. 对每个到期订单执行状态 CAS：`UPDATE orders SET status='CANCELLED_TIMEOUT' WHERE order_no=? AND status='CREATED'`。
4. 只有 CAS 命中的那次取消才继续：逐 SKU 调 `InventoryPort.release` 回补库存、写 `movement_type='TIMEOUT_RESTOCK'` 的 `inventory_movement` 证据（含 stock_before/after）、追加 `ORDER_TIMEOUT_CANCELLED` outbox 事件。全部在同一个事务里。
5. `cancel_deadline IS NULL` 的订单永不自动取消（测试夹具直接插库的订单属于这一类）。

## 为什么是 DB deadline 扫描，而不是 Redisson RDelayedQueue

| 维度 | DB deadline 扫描（选它） | Redisson 延迟队列 |
| --- | --- | --- |
| 可靠性 | 待取消状态就在订单行上，进程崩溃/重启不丢 | 队列消息在 Redis，宕机恢复语义要额外论证 |
| 幂等 | CAS 天然防重复取消与重复回补 | 消费端仍要做状态 CAS 兜底 |
| 依赖 | 零新增中间件（本 showcase 的 Redis 只用于 AI 限流） | 新增 Redisson 客户端与队列运维 |
| 及时性 | 秒级扫描对 30 分钟窗口足够 | 毫秒级，对本场景过剩 |

结论：在本项目的量级与边界下，扫描方案用更少的依赖换来同等的正确性；如果订单量放大或要求秒级精度，演进路径是把扫描降级为兜底、前置延迟队列加速，CAS 语义不变。

## 运营端触发

`POST /api/v1/operator/operations/order-timeout/enforce`（复用 `OperatorAuthorizationPolicy` 鉴权）立即执行一轮扫描并返回本次取消数，用于演示与验证，不需要等扫描周期。

## 测试

- 单元（Mockito）：取消到期订单逐 SKU 回补并发事件；并发已推进的订单被 CAS 跳过且不动库存；空扫描零交互。
- 集成（`@SpringBootTest` + H2）：到期订单取消后状态/库存/`TIMEOUT_RESTOCK` 证据/outbox 事件全部可查；重复执行不产生第二次回补或事件；已推进为 `PAID` 的订单保持库存不变；新订单拿到未来 deadline 不会被立即取消；运营端 API 在 DEMO_OPERATOR 下可触发。
- 迁移守卫测试（`flyway_schema_history` 计数）已从 11 更新为 12。

## 涉及的代码

- `apps/mall-api/src/main/resources/db/migration/V12__add_order_timeout_cancellation.sql`
- `order/application/port/out/OrderWritePort.java`（`findTimeoutDue` / `cancelIfCreated` / `storedItems` / `recordTimeoutRestock`）
- `inventory/application/port/out/InventoryPort.java`（`release`）
- `outbox/application/port/out/OutboxEventPort.java`（`OrderTimeoutCancelledEvent`）
- `order/timeout/OrderTimeoutCancellationService.java`、`OrderTimeoutSchedulingConfig.java`
- `operations/OperatorOrderTimeoutController.java`
