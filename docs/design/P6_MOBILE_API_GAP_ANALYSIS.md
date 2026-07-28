# P6 移动端 API Gap Analysis

状态定义：

- `READY`：现有真实接口或字段可直接用于移动端。
- `NEEDS_SMALL_CHANGE`：业务能力已有，但移动端契约或配置需要小范围适配。
- `MISSING`：当前没有可证明的真实接口，必须先设计并实现。
- `OUT_OF_SCOPE`：P6 不做，不以假数据替代。

| 功能/接口 | 状态 | 现状与 P6 处理 |
| --- | --- | --- |
| `GET /api/products` | READY | 返回商品、分类、图片路径、嵌套 SKU、价格、币种、库存。 |
| `GET /api/products/{productId}` | READY | Java 已有详情接口；P6B 接入商品详情页。 |
| `GET /api/cart?userId=1` | READY | Java 已有持久化查询；移动端改为读取服务端真相。 |
| `POST /api/cart/items?userId=1` | READY | 已有新增接口；数量变化、删除能力需先核对实际 API。 |
| Cart update/delete | MISSING | 当前 Controller 只有 GET 和 POST add。P6B 需要最小接口前先写 ADR/契约，不在本轮补代码。 |
| `POST /api/orders?userId=1` | READY | Header 必须是 `Idempotency-Key`，请求体为 SKU 数量列表。 |
| `GET /api/orders?userId=1` | READY | 可用于真实订单列表。 |
| `GET /api/orders/{orderNo}` | READY | 可用于下单成功后的详情。 |
| OrderItem 图片快照 | READY | `imagePathSnapshot` 已在 V7/Java DTO 中存在；移动端只读展示。 |
| `Idempotency-Key` | READY | Java/MySQL 已实现相同 Key 重放和不同请求体冲突语义。移动端只负责生成、复用和展示结果。 |
| 库存不足 | READY | 服务端原子扣减和事务回滚已存在，移动端需把错误映射为可理解的重试状态。 |
| `POST /api/ai/customer-service/ask` | READY | P4 已锁定新版契约；移动端需传真实 `productId`、`skuId` 和问题。 |
| Redis 限流头 | NEEDS_SMALL_CHANGE | Java 已暴露 `X-RateLimit-Mode`、`X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset`、`Retry-After`；移动 wrapper 需保留 headers。 |
| HTTP 429 | NEEDS_SMALL_CHANGE | 后端契约已存在；移动端需解析 `Retry-After` 和 body，不伪造答案。 |
| FAIL_OPEN | NEEDS_SMALL_CHANGE | Java 有 `DEGRADED` 语义；移动端需要显示“限流保护降级”而非把它当成 Redis 正常。 |
| AI 历史/Trace 查询 | MISSING | 当前没有移动端需要的 trace history/read API；P6C 只展示当前响应中的简化 Evidence。 |

## 不改写冻结后端

P6A 不修改 Java 订单、库存、幂等、AI、Redis、Flyway。P6B 优先消费 READY 接口；只有 Cart update/delete 或明确移动端阻塞项出现时，才以单独 ADR 评估最小后端变化。不能因移动端视觉参考而添加支付、物流、会员或认证状态机。

