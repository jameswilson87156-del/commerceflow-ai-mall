# P6A 当前移动端基线

## 审查范围

- 审查日期：2026-07-29
- 项目：`D:\workhome\commerceflow-ai-mall`
- 分支起点：`feat/p5-ai-rate-limit-final-freeze`
- 起点 HEAD：`58792a73b8ff8b8ed95f2a1df45f552cb9b091b3`
- 本轮分支：`feat/p6-mobile-commerce-research`
- 本轮只读审查、联网研究和规划文档编写，不修改业务代码、数据库、配置、截图或 CI。

## 移动端真实文件树

当前 `apps/mobile-app/` 只有一个最小 UniApp/Vue3/Vite 工程：

```text
apps/mobile-app/
├─ index.html
├─ package.json
├─ package-lock.json
├─ README.md
├─ vite.config.ts
└─ src/
   ├─ App.vue
   ├─ main.ts
   ├─ manifest.json
   ├─ pages.json
   ├─ uni-vue-compat.ts
   └─ pages/index/index.vue
```

没有 `components/`、`stores/`、`api/`、`types/`、`tests/` 或移动端图片资源目录。没有 Pinia、axios、状态持久化或移动端单元测试。

## 当前页面与交互

| 区域 | 当前事实 | 结论 |
| --- | --- | --- |
| Mall | 同一页面内的 `tab === 'mall'` 状态，调用 `GET /api/products` | 真实商品列表，但仍是原型页 |
| 商品详情 | 同页底部 sheet，直接使用列表响应中的嵌套 SKU | SKU 选择数据真实；没有独立详情路由 |
| 购物车 | `ref<any[]>([])` 本地数组 | 不是 Java/MySQL 持久化购物车 |
| 下单 | 直接 POST 购物车数组，成功后清空本地数组 | 订单提交 API 真实，但没有确认、成功详情、失败重试或订单列表 |
| Orders | 底部文字，没有点击处理和 API 调用 | 假入口，不能作为真实订单页面证据 |
| AI | 调用已弃用的 `POST /api/ai/product-chat`，仅传 question/userId | 没有 productId/skuId/businessFacts/429/FAIL_OPEN UI |
| 图片 | 商品卡使用商品名首字母块 | 没有使用现有 `/assets/products/` 商品图片 |
| 状态 | `request()` 不检查 HTTP 状态，页面没有 loading/empty/error/retry 视图 | P6B 必须先建立统一请求状态 |

## 依赖与构建

`package.json` 当前版本：

- `@dcloudio/uni-app`: `3.0.0-alpha-5020120260710001`
- `@dcloudio/vite-plugin-uni`: `3.0.0-alpha-5020120260710001`
- Vue: `3.5.13`
- Vite: `5.2.8`
- TypeScript: `^5.8.3`
- 脚本：`npm run dev`、`npm run build`、`npm run build:uni`

仓库已有 CI 仅执行移动端 `npm run build` 和 `npm run build:uni`，没有移动端测试、真机验收或 API 合约测试。历史 README 记录过 H5 和 UniApp 编译通过，但不能替代 P6 新实现的逐项证据。

## 当前 Java API 基线

| 能力 | 真实接口 | 当前判断 |
| --- | --- | --- |
| 演示登录 | `POST /api/auth/demo-login` | 可复用；仅演示账号，不是完整认证 |
| 商品列表 | `GET /api/products` | READY |
| 商品详情 | `GET /api/products/{id}` | READY；当前移动端未调用 |
| 购物车读取 | `GET /api/cart?userId=1` | READY；当前移动端未调用 |
| 购物车增加 | `POST /api/cart/items?userId=1` | READY；当前移动端未调用 |
| 订单提交 | `POST /api/orders?userId=1` + `Idempotency-Key` | READY；当前页直接调用 |
| 订单列表 | `GET /api/orders?userId=1` | READY；移动端未调用 |
| 订单详情 | `GET /api/orders/{orderNo}` | READY；移动端未调用 |
| AI 客服 | `POST /api/ai/customer-service/ask` | READY；移动端未调用，旧 `product-chat` 已弃用 |
| 限流 | `X-RateLimit-*`、`Retry-After` | Java/CORS 已实现；移动端暂无解析 |

商品数据来自 Flyway V1-V8 的本地 Showcase 数据。当前有两个商品，V4 增加到五个 SKU，并有 `cover_image_path`、`image_path`；V7 增加订单商品图片快照；V8 扩展 AI trace。库存事实来自 `inventory.available_stock`，订单状态当前只有 `CREATED`。

## P6A 结论

移动端完成度是“可构建的单页原型”，不是完整商城。最大技术问题不是缺少更多页面，而是业务事实没有统一进入移动端：本地 cart、未接入新 AI 契约、订单入口为空、请求错误不可见、localhost 无法支持手机设备。P6B 应先建立 API wrapper、真实商品详情、服务端购物车和订单结果链路，再考虑 P6C AI 入口。

