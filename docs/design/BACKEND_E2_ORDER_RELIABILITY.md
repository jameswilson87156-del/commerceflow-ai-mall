# CommerceFlow 后端 E2：订单端口与事务 Outbox 基础

> 状态：本轮已完成基础切片。日期：2026-09-03。本轮把订单编排与 JDBC 细节隔离，并实现 `ORDER_CREATED` 事务 Outbox 写入；尚未实现消息 Broker、后台发布 Worker 或外部通知。

## 1. 本轮目标

E2 不是把电商拆成多个微服务，而是先把最容易产生数据错误的订单写入链路收口：

1. 订单应用服务不直接依赖 `OrderRepository` 的具体类型；
2. 库存扣减通过端口完成，端口返回 `stockBefore/stockAfter` 供既有执行证据使用；
3. 幂等键查询通过持久化端口完成；
4. 在订单、订单快照、库存变动和购物车清理成功后，同一事务追加一个待投递事件；
5. 幂等记录先于库存预留写入，从而避免跨实例同 Key 竞争时先扣库存再发现重复。

## 2. 代码边界

```text
OrderService (@Transactional)
  ├─ OrderWritePort       -> OrderRepository adapter
  ├─ IdempotencyStore     -> OrderRepository adapter
  ├─ InventoryPort        -> JdbcInventoryAdapter
  └─ OutboxEventPort      -> JdbcOutboxEventAdapter
```

### 订单写端口

`OrderWritePort` 只暴露订单编排需要的事实：创建订单头、写入订单行快照、写入库存变动证据、清理购物车和读取创建结果。端口 DTO 是 `OrderDraft`、`OrderLine`，避免应用服务把 JDBC 行映射对象传播到其他模块。

### 库存端口

`InventoryPort.reserve(skuId, quantity)` 在调用方事务中执行：

- `SELECT ... FOR UPDATE` 读取扣减前库存；
- 带 `available_stock >= quantity` 条件的原子更新；
- 读取扣减后库存；
- 返回 `Reservation`，交给订单模块写入 `inventory_movement`。

缺货、库存行不存在和非法数量都映射为明确的业务异常。原有非负库存、确定性 SKU 排序、重复 SKU 聚合和回滚语义保持不变。

### 幂等边界

`orders` 表继续是当前幂等事实源，唯一键为 `(user_id, idempotency_key)`。订单服务现在先完成商品事实解析和金额计算，再尝试插入订单头；只有成功取得幂等记录后才开始库存预留。数据库唯一键竞争时，服务会再次查询已提交记录：同指纹返回原订单，不同指纹返回 `IDEMPOTENCY_KEY_REUSED`，未能读到竞争结果才返回 `IDEMPOTENCY_IN_PROGRESS`。

这条顺序修复了一个重要边界：跨 JVM 竞争不能在“发现重复订单”之前留下额外库存扣减。当前进程内的短生命周期 `inFlight` 防护仍保留，用于更快返回同 Key 的处理中状态；真正的持久化一致性由数据库唯一键负责。

## 3. Outbox 契约

Flyway V10 新增 `outbox_event`：

| 字段 | 当前语义 |
| --- | --- |
| `event_id` | `order-created:{orderNo}`，确定性去重身份 |
| `aggregate_type` / `aggregate_id` | `ORDER` / 订单号 |
| `event_type` | `ORDER_CREATED` |
| `payload_json` | 有界订单号、用户、金额、币种和订单行摘要 |
| `status` | 初始为 `PENDING` |
| `attempts` | 初始为 `0` |

`JdbcOutboxEventAdapter.append` 使用 Spring 的 `ObjectMapper` 序列化结构化事件，并通过同一个 `JdbcTemplate` 写入当前事务。当前没有把完整 Prompt、Authorization、Provider 响应或数据库秘密放进事件 payload。

事务顺序为：

```text
商品事实/金额校验
  -> 插入订单头（取得幂等记录）
  -> 按 SKU 顺序预留库存
  -> 订单行快照 + 库存变动证据 + 购物车清理
  -> PENDING ORDER_CREATED Outbox
  -> commit
```

任意一步失败都会回滚订单头、库存、订单行、库存证据、购物车清理和 Outbox 行。成功提交只说明事件已经可靠地落在同库 Outbox 中，不说明已经被外部系统消费。

## 4. 测试和退出边界

- `OrderReliabilityPortTest` 固定订单服务依赖的是端口而非 `OrderRepository` 具体类型，并保护库存/购物车/证据所需的写端口事实。
- `OrderOutboxTests` 覆盖成功写入 `PENDING` 事件、同 Key 重放不追加第二事件、缺货失败不留下 Outbox 行。
- 既有订单测试继续覆盖同 Key 重放、不同请求体冲突、重复 SKU、并发库存扣减、库存不足、部分扣减回滚和证据读取。
- 从仓库根目录执行 `./mvnw.cmd -f apps/mall-api/pom.xml test`，当前 Java 回归为 `65` 条测试通过。

本轮明确没有完成：Outbox claim/lease、发布 Worker、重试退避、死信、Broker、通知、搜索索引、支付回调或跨服务事务。E3 已继续处理 AI Provider SPI 与错误/可观测性边界；Outbox 消费者可在 staging 需要真实副作用时单独加 E2.1。
