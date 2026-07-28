# P6 移动端状态设计

## 选择原则

P6B 初始实现可以使用 Vue3 Composition API composable，避免为一个页面原型立即引入 Pinia。若商品、购物车、订单成功页、订单列表和 AI 页面跨路由共享状态变多，再评估 Pinia。是否使用 Pinia 由实际跨页面共享复杂度决定，不由关键词决定。

## 状态分层

| 状态 | 所有者 | 规则 |
| --- | --- | --- |
| catalog/detail | 页面 composable + API | server data；进入/返回时按页面生命周期刷新。 |
| selected SKU | 商品详情页面 | UI selection；提交前再次以 API/库存错误为准。 |
| cart | Java/MySQL | 服务端真相；本地仅缓存最后视图或恢复提示。 |
| order submit | submit composable | 保存一次 `Idempotency-Key`，loading 期间禁止重复提交。 |
| order success/detail | URL/页面参数 + API | 只用返回的 orderNo 和 `GET /api/orders/{orderNo}`。 |
| AI conversation | 当前页面内存 | P6C 不做真实历史持久化，不把聊天当订单事实。 |
| rate limit | AI request wrapper/composable | 保存 headers、429、cooldown；不复用旧请求的 Evidence。 |

## 返回与恢复

- 页面 `onShow` 可刷新商品、购物车和订单列表。
- 从订单成功返回商品页时不假设购物车已清空，应该重新读取服务端 cart。
- 请求取消、网络失败和业务错误要有不同的 UI 状态。
- 本地持久化不得保存真实密钥、完整支付信息或伪造订单；演示用户 ID 只能来自明确 demo session。

