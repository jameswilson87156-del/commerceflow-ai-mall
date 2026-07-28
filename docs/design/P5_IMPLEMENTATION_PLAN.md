# P5B 实施计划

P5B 才执行下列工作，并先在新分支审计 P5A 决策是否仍适用。

1. 在 `pom.xml` 加入 Redis starter 与 Testcontainers 测试依赖，锁定实际解析版本和许可证。
2. 在 `application.yml`、`.env.example`、Compose 和 PowerShell 脚本增加 Redis 本地运行、healthcheck 与安全默认配置。
3. 新增配置属性、身份 hash、`AiRateLimiter`、Lua resource、限流决策对象及专用 429/503 异常映射。
4. 在 AI 基本格式校验后、Catalog 查询前接入限流；不改 Python 合约、MySQL schema/Flyway、订单/库存/P2/P3/P4 业务逻辑。
5. 为正常、429、fail-open、fail-closed、并发、TTL、隐私键和 P4 回归增加 Java Testcontainers 测试。
6. 让 Vue 解析响应头与 429 body，实现倒计时和降级提示，补 Vitest 和浏览器验收。
7. 更新 README、故障排查、Evidence、截图和面试说明，仅在真实运行后生成证据。

明确不做：Redis 缓存、session/cart、锁、MQ、MyBatis 改造、微服务、订单功能、真实付费 AI、生产发布。P5B 不应有 Flyway 迁移，除非后续出现独立且经批准的配置持久化需求；当前设计没有该需求。
