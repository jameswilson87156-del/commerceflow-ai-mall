# P5 限流测试计划

P5A 未运行 Redis 测试；以下是 P5B 必须完成的验收计划。

## Java（至少 18 项）

1. 首次有效请求允许并返回额度头。
2. 窗口内第 2 到第 5 次允许，`remaining` 递减。
3. 第 6 次为 429，含 `Retry-After` 和安全 JSON。
4. 窗口结束后可再次请求。
5. 原子脚本返回的计数、TTL、reset 一致。
6. 相同身份、不同 SKU 共享 AI 入口额度。
7. 不同 identity hash 互不影响。
8. 空/格式无效请求不消耗额度。
9. 产品不存在会消耗一次，但不调用 Python。
10. Python timeout 消耗一次并维持现有 Java fallback。
11. Provider fallback 消耗一次。
12. 429 不调用 Catalog、Python、Trace 保存。
13. 并发请求总允许数不超过 limit。
14. Redis 返回异常时 `FAIL_OPEN` 继续且 header 标识 degraded。
15. `FAIL_CLOSED` 配置下返回受控 503，不泄漏 Redis 细节。
16. key 不含原始 IP、问题、productId、skuId、clientRequestId 或 secret。
17. 配置上下界和非法值验证。
18. 既有 P4 AI、Evidence、Trace、fallback 回归。

优先采用 Testcontainers 的真实 Redis 集成测试；Docker Compose 仅用于本地浏览器验收。Mock 只用于 Redis 故障分支，不应替代原子与 TTL 验证；不采用 embedded Redis。

## Vue（至少 10 项）

1. 解析成功额度头。 2. 显示 remaining/reset。 3. 429 显示中文等待信息。 4. 保留问题。 5. 倒计时禁用发送。 6. 倒计时结束恢复。 7. 手动重试发起新请求。 8. degraded 提示安全。 9. 非 429 通用错误保持。 10. 单击期间不得重复请求。

## 浏览器

真实 Redis、Java、Vue 下连续请求至 429、等待 TTL、换本地身份、停止 Redis 验证 fail-open、恢复 Redis；记录 Network 头、HTTP 状态和控制台 0 错误。
