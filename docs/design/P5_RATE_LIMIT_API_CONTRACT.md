# P5 AI 限流 HTTP 契约

范围只限 `POST /api/ai/customer-service/ask`。RFC 6585 定义 429 用于在给定时间内请求过多，且可用 `Retry-After` 指示等待时间；429 不应被缓存。[RFC 6585](https://www.rfc-editor.org/rfc/rfc6585.html#section-4)

## 允许的请求

在 Redis 正常且被允许时，保留既有成功体，并添加：

- `X-RateLimit-Limit: 5`
- `X-RateLimit-Remaining: 0..4`
- `X-RateLimit-Reset: <UTC epoch seconds>`
- `X-RateLimit-Mode: redis`

`Reset` 是窗口结束的 Unix epoch 秒，不是模糊的相对秒数。

## 被拒绝的请求

HTTP `429`，并添加 `Retry-After: <positive integer seconds>`、同一组 `X-RateLimit-*` 和 `X-RateLimit-Mode: redis`。JSON 在现有 `{code,message}` 约定上扩展为：

```json
{
  "code": "AI_RATE_LIMIT_EXCEEDED",
  "message": "请求过于频繁，请在 42 秒后重试。",
  "retryAfterSeconds": 42,
  "limit": 5,
  "remaining": 0
}
```

不得返回 Redis key、完整 IP、身份 hash、脚本内容、连接细节或堆栈。P5B 应用专用异常与显式 `ResponseEntity`/异常处理映射，不能靠字符串匹配 HTTP 状态。
