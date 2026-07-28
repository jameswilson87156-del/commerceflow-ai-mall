# P5 限流算法决策

## 决策

P5B 对 `POST /api/ai/customer-service/ask` 采用**Redis Lua 原子固定窗口**：默认每个演示身份每 60 秒最多 5 个已通过基本格式校验的请求。

## 理由

- 目标是削减重复调用与未来外部模型成本，不是构建通用网关。
- 一个短期字符串计数器足够；Lua 将 `INCR`、首次 `EXPIRE`、剩余额度和 TTL 放在一次原子执行中，避免应用端 `INCR`/`EXPIRE` 两步之间的 TTL 遗漏。
- 固定窗口的边界突发是已知、可接受的 Showcase 取舍；ZSET 滑动窗口和 token bucket 不是当前需求。

## 不采用

- 不采用无脚本的 `INCR` + `EXPIRE`。
- 不采用 ZSET、Redis Functions、复杂限流框架或 Bucket4j。
- 不对订单、库存、购物车、登录、产品查询、Python 内部接口或其他路径限流。

P5B 应把脚本封装在专用 `AiRateLimiter`，而非散落于 Controller、Service 或 MyBatis；脚本实现和返回值必须有单元与 Redis 集成测试。
