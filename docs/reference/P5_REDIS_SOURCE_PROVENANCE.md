# P5 Redis 研究来源与可追溯性

研究日期：2026-07-28。

| 来源 | 用途 | 版本/提交锚点 | 复用情况 |
| --- | --- | --- | --- |
| [Redis INCR](https://redis.io/docs/latest/commands/incr/) | 计数器语义 | 官方最新文档 | 只引用命令语义，不复制代码 |
| [Redis EVAL](https://redis.io/docs/latest/commands/eval/) | 脚本调用形态 | 官方最新文档 | 不复制代码 |
| [Redis Lua scripting](https://redis.io/docs/latest/develop/programmability/eval-intro/) | `KEYS`/`ARGV` 及显式键约束 | 官方最新文档 | 不复制代码 |
| [Spring Boot Redis](https://docs.spring.io/spring-boot/reference/data/nosql.html) | Boot 自动配置、Lettuce、`StringRedisTemplate` | 项目当前 Boot `3.4.5` | 不复制示例 |
| [RFC 6585 section 4](https://www.rfc-editor.org/rfc/rfc6585.html#section-4) | HTTP 429、`Retry-After`、不可缓存 | RFC 6585 | 只采用协议语义 |
| [Redis license](https://redis.io/legal/licenses/) | Redis 8 三选一许可 | 官方最新页面 | 不复制许可文本 |
| [Bucket4j](https://github.com/bucket4j/bucket4j) | token bucket 候选 | release `8.19.0`；master `92553c54b47d7308095ed4c438a7a170371a400f` | 不引入、不复制 |
| [Redis 8 branch](https://github.com/redis/redis/tree/8.0) | Redis 8 许可/分支锚点 | `f49e5cb2b161418f92f2095c532b85ad24d80819` | 不复制 |

P5A 新增文档为 CommerceFlow 原创设计文字。P5B 的 Lua 脚本须由本项目重新实现、最小化并随测试提交；不得从无明确许可证的博客或仓库复制。任何新依赖、镜像 tag、许可证变化均须更新本文件和许可证矩阵。
