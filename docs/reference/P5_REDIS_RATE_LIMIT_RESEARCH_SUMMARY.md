# P5 Redis AI 限流研究总结

## 已锁定答案

1. **目标**：只保护 `POST /api/ai/customer-service/ask`，减少重复调用与未来模型成本。
2. **算法**：Lua 原子固定窗口，5 次/60 秒；接受边界突发，拒绝复杂滑动窗口和 token bucket。
3. **身份**：本地 `userId + server remote address` 的 HMAC 短 hash；不信任 XFF，不把它视为生产认证。
4. **数据**：Redis 只保存有 TTL 的整数计数器，不保存 P4 业务或证据数据。
5. **失败**：当前 mock Showcase 默认 FAIL_OPEN，响应只暴露 `degraded`，不泄漏 Redis 细节。
6. **HTTP**：成功带 `X-RateLimit-Limit/Remaining/Reset`；拒绝返回 429、`Retry-After` 和最小 JSON。
7. **位置**：基本格式校验后、业务事实查询前；429 不调用 Python 或写 Trace。
8. **前端**：保留问题，解析 429，倒计时禁用，禁止自动重试。
9. **测试**：P5B 用 Testcontainers 真实 Redis 验证原子性与 TTL；Vue 验证可观察交互。
10. **运行**：未来本地 Compose 使用 Redis 8.0 系列、healthcheck、无持久卷；P5A 不启动它。
11. **许可证**：Redis 8 的三选一许可需在 P5B 精确镜像锁定前再复核；Bucket4j 仅为 Apache-2.0 候选。
12. **不做**：缓存、锁、队列、session/cart、多实例、网关、生产认证、计费、订单或库存改造。
13. **事实来源**：MySQL 和既有 Java 链路不变；Redis 不是事实来源。
14. **安全**：不存储/暴露原始 IP、问题、key、secret 或业务数据。
15. **下一步**：用户批准 P5A 后才进入 P5B 实施分支；本分支只提交设计文档且不推送。

关键外部依据： [Redis commands](https://redis.io/docs/latest/commands/incr/)、[Redis Lua](https://redis.io/docs/latest/develop/programmability/eval-intro/)、[Spring Boot Redis](https://docs.spring.io/spring-boot/reference/data/nosql.html)、[RFC 6585](https://www.rfc-editor.org/rfc/rfc6585.html#section-4)、[Redis licenses](https://redis.io/legal/licenses/)。
