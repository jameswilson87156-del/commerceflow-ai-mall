# P5 Redis 许可证矩阵

本文件是依赖与代码来源审查，不是法律意见。P5A 不新增任何依赖或容器镜像。

| 项目 | P5B 拟用/候选 | 许可证 | 决策 | 来源 |
| --- | --- | --- | --- | --- |
| Redis Open Source | `redis:8.0` 系列，P5B 再锁定精确 patch 和 digest | Redis 8 为 RSALv2 / SSPLv1 / AGPLv3 三选一 | 仅作本地 Showcase 短 TTL 计数器；P5B 前重新审查镜像 tag、digest 与适用许可 | [Redis licenses](https://redis.io/legal/licenses/) |
| Spring Boot / Spring Data Redis / Lettuce | `spring-boot-starter-data-redis`，由现有 Boot 3.4.5 BOM 管理 | Apache-2.0（需在 P5B 引入时以实际 Maven 解析结果复核） | 可采用，优先 `StringRedisTemplate`，不复制实现 | [Spring Boot Redis reference](https://docs.spring.io/spring-boot/reference/data/nosql.html) |
| Testcontainers | P5B 测试候选 | Apache-2.0（P5B 加依赖时复核） | 推荐仅测试范围使用真实 Redis 容器 | [Testcontainers repository](https://github.com/testcontainers/testcontainers-java) |
| Bucket4j | 仅对比候选；最新审计 release 为 `8.19.0` | Apache-2.0 | 本期不引入；不复制源码 | [Bucket4j repository](https://github.com/bucket4j/bucket4j) |

Redis 8 许可与 Redis 7.2 及以前的 BSD-3-Clause 不同，不能沿用旧假设。P5B 如选择 AGPLv3 必须进行单独许可证复核；不得把“本地演示”误写成已完成商业许可评估。
