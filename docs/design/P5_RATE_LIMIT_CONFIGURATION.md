# P5 限流配置

P5B 采用 `@ConfigurationProperties` 集中绑定，禁止在业务方法散落数字。

| 属性 | 本地默认 | 验证范围 | 说明 |
| --- | --- | --- | --- |
| `commerceflow.ai.rate-limit.enabled` | `true` | boolean | `false` 时明确不执行 Redis 限流，仅限本地演示 |
| `commerceflow.ai.rate-limit.limit` | `5` | 1-60 | 每窗口允许次数 |
| `commerceflow.ai.rate-limit.window-seconds` | `60` | 1-3600 | 固定窗口秒数 |
| `commerceflow.ai.rate-limit.key-prefix` | `commerceflow:local:ai:rate:v1` | 非空、受控字符、最长 80 | Redis 键前缀 |
| `commerceflow.ai.rate-limit.failure-policy` | `FAIL_OPEN` | `FAIL_OPEN` / `FAIL_CLOSED` | Redis 故障策略 |
| `commerceflow.ai.rate-limit.identity-hash-secret` | 本地演示值，仅由环境变量提供 | 非空；非 local 不可用默认值 | HMAC 秘密，不回显 |

Redis 连接使用 Spring Boot `spring.data.redis.host`、`port`、`database`、timeout 配置；P5B 默认数据库为 0。测试应使用单独 prefix 和短窗口，不依赖睡眠来碰巧通过。配置非法时启动失败，而不是静默退化为未知配额。
