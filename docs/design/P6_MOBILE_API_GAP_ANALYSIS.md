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
| `GET /api/v1/me/cart` | READY | Phase 0-B 已由 `CurrentUserPort` 派生用户范围；移动端不传 `userId`。 |
| `POST/PUT/DELETE /api/v1/me/cart/items...` | READY | 购物车新增、数量变化、删除均使用服务端当前用户范围。 |
| Cart update/delete | READY | Phase 0-B 版本化购物车 Controller 已提供更新和删除，均从 `CurrentUserPort` 派生用户范围。 |
| `POST /api/v1/me/orders` | READY | Header 必须是 `Idempotency-Key`，请求体为 SKU 数量列表，用户由服务端派生。 |
| `GET /api/v1/me/orders` | READY | 只返回服务端当前用户范围内的真实订单。 |
| `GET /api/v1/me/orders/{orderNo}` | READY | 当前用户范围外的订单按 `404` 不可见处理。 |
| OrderItem 图片快照 | READY | `imagePathSnapshot` 已在 V7/Java DTO 中存在；移动端只读展示。 |
| `Idempotency-Key` | READY | Java/MySQL 已实现相同 Key 重放和不同请求体冲突语义。移动端只负责生成、复用和展示结果。 |
| 库存不足 | READY | 服务端原子扣减和事务回滚已存在，移动端需把错误映射为可理解的重试状态。 |
| `POST /api/v1/me/ai/customer-service/ask` | READY | Phase 0-B 请求体只传 `productId`、`skuId`、问题和 `clientRequestId`；用户范围由 `CurrentUserPort` 派生。 |
| Redis 限流头 | NEEDS_SMALL_CHANGE | Java 已暴露 `X-RateLimit-Mode`、`X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset`、`Retry-After`；移动 wrapper 需保留 headers。 |
| HTTP 429 | NEEDS_SMALL_CHANGE | 后端契约已存在；移动端需解析 `Retry-After` 和 body，不伪造答案。 |
| FAIL_OPEN | NEEDS_SMALL_CHANGE | Java 有 `DEGRADED` 语义；移动端需要显示“限流保护降级”而非把它当成 Redis 正常。 |
| AI 历史/Trace 查询 | MISSING | 当前没有移动端需要的 trace history/read API；P6C 只展示当前响应中的简化 Evidence。 |

## 当前边界

移动端消费的 `/api/v1/me/...` 路径不信任客户端 `userId`；Admin/Operator 的跨用户读取使用独立 `/api/v1/operator/...`。旧 `/api/...` 路径只作为显式 LOCAL/DEMO 兼容接口保留。不能因移动端视觉参考而添加支付、物流、会员或认证状态机；真实登录、OIDC/JWT 和生产 RBAC 仍是后续工作。
