# P5 限流身份与键决策

## 决策

P5 Showcase key 使用 `HMAC-SHA-256(normalizedUserId + "|" + remoteAddress, identityHashSecret)` 的前 24 个小写十六进制字符。`remoteAddress` 来自服务器直接连接的 `HttpServletRequest.getRemoteAddr()`；不信任 `X-Forwarded-For` 或其他转发头。

`userId` 仍来自当前请求体，因而不是生产认证身份。加入服务器看到的远端地址只改善本地演示中的简单区分，不构成安全认证。未来有认证后，改为认证 subject；在可信反向代理部署前，才另行 ADR 定义代理地址提取。

## 不存储和不暴露

- Redis key、日志、Trace 和错误体不写完整 IP、原始 `userId` 拼接值、问题、商品、SKU、API Key 或 hash secret。
- 只写短期 hash 计数器；hash 不是匿名化承诺，秘密必须在非本地环境用安全配置提供。
- 不将 `clientRequestId` 作为限流身份或幂等机制；它只是当前链路关联字段。

身份候选对比：仅 userId 可伪造；仅 IP 会受 NAT/代理影响；`userId + server remote address` 是 P5 本地折中；认证 subject 是未来正确方案。
