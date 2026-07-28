# P6 CORS 配置设计

## 当前实现审查

当前 `WebConfig` 对 `/api/**` 允许固定的 localhost/127.0.0.1 5173、5174、5175 origin，允许 GET/POST/OPTIONS 和任意请求头，并暴露 P5 的限流响应头。P6A 不修改 Java。

## 后续设计

1. 将允许 origin 改为环境配置的白名单，分别覆盖本地 admin、移动 H5 和可选部署域名。
2. 不使用 `allowedOrigins("*")`，不把凭证和任意来源一起放开。
3. 只开放实际需要的方法和 headers；移动端需要 `Idempotency-Key`、`Content-Type`，AI 页面需要读取限流头。
4. 保留并测试 `Access-Control-Expose-Headers: X-RateLimit-Mode, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, Retry-After`。
5. 原生 App 请求不受浏览器 CORS 同样约束，但仍需 API 地址、网络权限和证书可达；小程序还需要平台域名白名单。
6. 配置变更必须单独测试 OPTIONS、真实 H5 origin 和非白名单 origin。

## 不属于本轮

本轮不修改 `WebConfig`、环境配置或 Java 测试；这份文档只锁定 P6B/P6C 的实施前提。

