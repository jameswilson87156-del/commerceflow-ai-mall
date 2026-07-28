# P5 Redis 键与脚本设计

键格式：`commerceflow:local:ai:rate:v1:{identityHash}:<windowStartEpochSeconds>`。其中 `identityHash` 是 24 位 HMAC 截断值，`windowStartEpochSeconds=floor(now/windowSeconds)*windowSeconds`。值是整数计数；TTL 是到窗口结束的秒数。

Lua 输入只能有一个完整 `KEYS[1]` 和普通 `ARGV`（limit、TTL）。脚本必须：递增；首次计数设置 TTL；读取 TTL；返回 `allowed`、`count`、`remaining`、`ttlSeconds`。该过程在单个 `EVAL` 中完成。若异常 TTL 被发现，脚本应在同一执行内修复或抛出受控错误，由 `failure-policy` 处理。

不在 Redis 中保存问题、商品名、商品/SKU ID、库存、订单、答案、Evidence、Trace、完整 IP、原始用户 ID、clientRequestId、API Key 或密码。只使用 database 0；不使用缓存、锁、队列、session 或 cart 数据结构。

P5B 需将自有 Lua 文本放在受版本控制的 Java resources 中，并将 SHA/语义记录在测试中；本 P5A 不创建脚本文件。
