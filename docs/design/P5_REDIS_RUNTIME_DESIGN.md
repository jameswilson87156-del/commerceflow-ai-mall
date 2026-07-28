# P5 Redis 运行设计

P5B 才在现有 Compose 中新增官方 Redis Open Source `redis:8.0` 系列，并在实现时锁定精确 patch 与 digest。建议仅本地映射 `${REDIS_PORT:-6380}:6379`，healthcheck 使用 `redis-cli ping`，无持久卷、不开 AOF：限流键都应由 TTL 自动过期。

- Java 使用 Boot 自动配置的 Lettuce / `StringRedisTemplate`，配置在 `spring.data.redis.*`；连接和命令超时必须较短且单独配置。
- `.env.example` 仅补 host/port、限流开关和非机密 local 示例；不提交真实密码、外部服务 URL 或密钥。
- 脚本应提供 `start-redis` 或扩展现有开发脚本、healthcheck、故障演示和清理说明；CI 是否启 Redis 由 P5B 的 Testcontainers 决定。
- 不为本地 Showcase 假装生产安全：不声明 TLS、HA、哨兵、集群、持久化、托管 Redis 或公网部署。

Spring Boot 可自动配置 Lettuce、Jedis 和 Spring Data Redis 抽象，并支持 `StringRedisTemplate` 与 `spring.data.redis.*` 连接属性。[Spring Boot Redis](https://docs.spring.io/spring-boot/reference/data/nosql.html)
