# P6 实施计划

## P6A 研究与锁定（本分支）

- 分支：`feat/p6-mobile-commerce-research`
- 只新增本批 `docs/` 文档。
- 产出：基线、API gap、地址/CORS/状态设计、平台范围、测试/CI计划、来源和许可证记录。
- 停止点：人工确认范围；不得自动进入代码实现。

## P6B 电商核心

- 建议分支：`feat/p6b-mobile-commerce-core`
- 页面：`pages/catalog/index`、`pages/product/detail`、`pages/cart/index`、`pages/order/confirm`、`pages/order/success`、`pages/orders/index`、`pages/orders/detail`。
- 代码：统一 `api/request`、商品/cart/order types、图片 URL resolver、最小 composables/state。
- 后端：优先只消费现有商品、购物车、订单 API；若需 cart update/delete，单独 ADR 评估最小接口。
- 测试：列表、SKU、库存、图片、cart、幂等、库存不足、订单状态和全部 loading/error/retry。
- 浏览器：390x844/430x932，真实 API，中文页面，控制台 0。
- 停止点：订单成功必须来自真实 orderNo；任何支付/物流需求都停止并回到范围审查。

## P6C AI 客服入口

- 建议分支：`feat/p6c-mobile-ai-entry`
- 从商品详情带 productId/skuId 调用新版 `/api/ai/customer-service/ask`。
- 复用 Mock、fallback、Evidence/Trace、Redis remaining、429、FAIL_OPEN；不写数据库业务数据。
- 测试正常、fallback、429、倒计时、重试和上下文正确性。
- 停止点：不增加 AI 历史、真实 Key、跨服务业务写入。

## P6D 截图与求职证据

- 建议分支：`feat/p6d-mobile-evidence`
- 只记录真实命令、API、浏览器网络、控制台、截图和平台编译结果。
- 先 H5 截图，再 UniApp 编译证据；没有真机则明确标记未验收。
- 不把旧移动截图或 AI 参考图当作新实现证据。

