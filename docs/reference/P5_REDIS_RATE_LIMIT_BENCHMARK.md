# P5 Redis 限流方案对比

研究日期：2026-07-28；仅使用官方文档与候选项目官方仓库。结论针对单体 Showcase 的单个 AI 问答入口，不宣称生产吞吐或压测结果。

| 方案 | 精度与突发 | 原子性 | 运行成本 | P5 结论 |
| --- | --- | --- | --- | --- |
| A. `INCR` + `EXPIRE` 固定窗口 | 简单；窗口边界可出现最多两窗口叠加突发 | 两个命令间有失败窗口 | 一个字符串键 | 不采用：TTL 首写不能只靠应用端两次命令保证 |
| B. Lua 原子固定窗口 | 与 A 相同的边界突发；本 P5 5 次/60 秒可接受 | 一个 `EVAL` 中完成计数、首次 TTL、剩余量 | 一个短 TTL 键、一次脚本调用 | **采用** |
| C. ZSET 滑动窗口 | 更严格的任意时间段限制 | 清理、计数、写入必须脚本化 | 每请求保留时间戳，需清理 | 不采用：精度收益不足以抵消复杂度 |
| D. Token bucket（如 Bucket4j） | 支持可配置 burst 与补充速率 | 需选择 Redis 分布式后端与一致性策略 | 新库、序列化和运维面 | 不采用：当前没有 burst 需求，避免额外依赖 |

Redis 的 `INCR` 对整数键是 O(1) 递增；Lua 脚本通过 `EVAL` 接收显式 `KEYS` 和 `ARGV`，适合把计数与 TTL 合并为一个 Redis 操作。Redis 官方要求脚本访问的键以输入键显式传入，因此 P5B 的脚本只操作一个完整限流键，不在 Lua 内拼接未知键名。[Redis INCR](https://redis.io/docs/latest/commands/incr/)；[Redis Lua scripting](https://redis.io/docs/latest/develop/programmability/eval-intro/)。

Bucket4j 是 Apache-2.0 的成熟 token-bucket 候选库，但本期不引入，也不复用其代码；如未来有突发配额或多策略需求，再通过 ADR 评估。[Bucket4j 官方仓库](https://github.com/bucket4j/bucket4j)
