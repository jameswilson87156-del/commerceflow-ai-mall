# P5 Redis 故障策略

## 决策：P5 Showcase 默认 FAIL_OPEN

当 Redis 连接、超时、脚本执行或返回格式失败时，Java 记录结构化安全日志和健康指标，并继续现有 Product/SKU/Inventory -> Python -> Evidence/Trace 链路。原因是当前默认 Provider 是无付费的确定性 mock；将 Redis 短暂故障升级为用户不可用会遮蔽 P4 已验证的 fallback。

响应可观察但不泄漏基础设施：成功响应带 `X-RateLimit-Mode: degraded`，前端仅显示“请求保护暂时降级，本次请求仍按本地演示链路处理”。不暴露 Redis 主机、端口、键、脚本、堆栈或完整 IP。

## 未来选择

- 真实付费 Provider 稳定接入前，通过 ADR 重新评估默认 `FAIL_CLOSED`。
- 不引入本地内存兜底：多实例语义不一致且会扩大 P5 范围。
- Redis 失效不影响 Java 基于业务事实的 AI fallback；已获准请求继续写既有 Evidence/Trace，未获准 429 不调用事实、Python 或 Trace。
