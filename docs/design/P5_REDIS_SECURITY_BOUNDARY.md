# P5 Redis 安全与事实边界

Redis 是短期限流辅助组件，不是 CommerceFlow 的业务事实来源。MySQL 继续拥有 Product、SKU、Inventory、Cart、Order、OrderItem、库存 movement、AI Trace 和 Evidence；Redis 不参与订单幂等、库存原子扣减、MySQL 事务、业务 facts、AI 回答、Evidence/Trace 主存储、会话或购物车。

P5 不引入产品缓存、分布式锁、消息队列、网关、多实例协调、生产认证、支付模型或计费。P5 身份仅用于本地 Showcase 限流，不能作为授权或审计身份。生产部署、可信代理、TLS、Redis ACL、密钥托管、监控告警和多实例语义均为未来单独决策。

所有 429 和 degraded 响应必须最小披露；日志也不得写 question、完整地址、Redis key、secret 或完整请求体。
