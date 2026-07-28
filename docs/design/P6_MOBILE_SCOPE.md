# P6 移动端业务范围

## P6B：电商核心

P6B 只完成一条真实、可复现、面向求职展示的移动链路：

1. 商品列表
2. 商品详情
3. SKU 颜色/尺寸选择
4. 数量调整与可购买性
5. 服务端购物车读取/增加（若 update/delete 缺失，先以 API Gap 单独决策）
6. 订单确认
7. `Idempotency-Key` 提交订单
8. 库存不足错误
9. `CREATED` 下单成功
10. 订单详情与订单列表

P6B 的所有价格、SKU、库存、订单号、时间和状态均来自 Java API/MySQL。客户端只负责交互和显示。

## P6C：移动 AI 客服

从商品详情进入 AI 客服，携带：

- `productId`
- `skuId`
- 商品名、SKU 编码、颜色、尺寸
- 价格、币种、`availableStock`
- 可购买性

调用新版 `POST /api/ai/customer-service/ask`，默认使用 `commerceflow-mock / MOCK`，保留 Provider、answer status、traceId、简化 Evidence、Redis remaining、429、FAIL_OPEN 语义。移动端不建立真实 AI 历史，不写商城业务数据。

## P6D：验收与材料

P6D 只做构建、测试、浏览器运行证据、移动端截图和面试说明。每一步记录真实命令和结果。

## 明确排除

本阶段不做注册、短信、真实认证、微信登录、支付、微信支付、支付宝、物流、发货、退款、地址、优惠券、积分、会员、营销、虚假订单状态、真实 AI Key、外网部署或新业务数据库表。

