# P5 限流执行顺序

P5B 的 `ask` 顺序固定如下：

1. HTTP JSON 和基本格式校验：正数 ID、非空问题、500 字限制、`clientRequestId` 格式。
2. 从已校验的 `userId` 和服务器远端地址构造短 hash 身份。
3. 调用 Redis 原子固定窗口限流。
4. 仅获准时查询 Product、SKU、Inventory 并构造 Java `businessFacts`。
5. 调用 Python；现有超时、非法 JSON、不可用和 Java facts fallback 逻辑保持。
6. 仅获准时保存现有 Evidence/Trace，返回正常成功响应和额度头。

格式无效请求不消耗额度；产品不存在、SKU 不存在或不匹配在第 3 步后才发现，因此会消耗一次有效 AI 入口尝试。Python 超时、Provider fallback 与不支持问题同样消耗一次，因为外部调用或 Java fallback 工作已开始。429 不查询业务事实、不调用 Python、不创建 Evidence/Trace；前端重试是新请求并再次计数，不自动重放。
