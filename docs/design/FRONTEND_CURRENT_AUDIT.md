# CommerceFlow 前端现状审查

审查日期：2026-09-04  
项目根目录：`D:/workhome/commerceflow-ai-mall`  
范围：`apps/mobile-app`、`apps/admin-web`，并同步核对 Phase 0-B 的 Java API 边界。本文不修改另一个项目；消费者和 Operator 路径以 Phase 0-B 设计为准。

## 1. 当前路由和页面

### UniApp H5 / 移动端

| 路由 | 页面 | 当前真实职责 |
| --- | --- | --- |
| `/pages/index/index` | 商品列表 | 调用 `GET /api/products`，展示真实在售商品、图片、SKU 数量、起售价和商品入口 |
| `/pages/product/detail` | 商品详情 | 调用 `GET /api/products/{productId}`，选择真实 SKU，展示库存状态，调用加购 API，进入 AI 客服 |
| `/pages/cart/index` | 购物车 | 调用购物车读取、更新、删除 API；使用整数分计算总价，进入订单确认 |
| `/pages/order/confirm` | 订单确认 | 重新读取服务端购物车，使用前端生成幂等键调用真实订单 API |
| `/pages/order/result` | 订单结果 | 读取真实订单详情，显示 `CREATED`、订单号、金额和边界说明 |
| `/pages/order/detail` | 订单详情 | 读取真实订单快照，展示历史商品、SKU、数量、单价和小计 |
| `/pages/order/list` | 订单列表 | 调用 `GET /api/v1/me/orders`，展示服务端当前用户的真实创建订单 |
| `/pages/ai/customer-service` | AI 商品客服 | 读取真实商品/SKU，调用 Java AI 接口，展示 Provider、限流、回答、Evidence、Trace 和 fallback |

H5 通过 `apps/mobile-app/src/App.vue` 和 `platform/h5.ts` 进行 hash 路由模拟；非 H5 目标当前只保留 UniApp 编译证据。

### Vue Admin

| 视图 | 页面 | 当前真实职责 |
| --- | --- | --- |
| `overview` | 运营总览 | 调用受 OperatorScope 保护的 `GET /api/v1/operator/operations/overview`，展示真实商品、SKU、库存、订单、AI Trace 和运行边界摘要 |
| `products` | 商品与 SKU | 调用商品列表和详情 API，提供本地搜索、分类/库存筛选和 SKU 只读检查 |
| `orders` | 订单与库存执行证据 | 调用订单列表、详情和 execution-evidence API，展示快照、幂等键、库存 before/after 和 movement |
| `ai` | AI 客服工作台 | 调用商品列表和受 OperatorScope 保护的 `POST /api/v1/operator/ai/customer-service/ask`，展示 Java 事实、Mock/Fallback、Evidence、Trace 和 Redis 配额 |

公共壳层在 `apps/admin-web/src/App.vue`，样式集中在 `apps/admin-web/src/style.css`，页面逻辑和 API helper 目前与页面同目录。

## 2. 真实 API、数据来源和边界

### Real

- Product、SKU、Inventory：Java API 从本地 MySQL 返回；图片使用仓库内已有商品素材。
- Cart：调用 `GET /api/v1/me/cart`，用户范围由服务端派生，前端不维护一份虚假购物车作为成功来源。
- Order：真实提交 `Idempotency-Key`，状态为 `CREATED`，订单明细是服务端快照。
- Order evidence：服务端返回库存变动、幂等结果和事务说明。
- AI：消费者前端只提交 `productId`、`skuId`、`question`、`clientRequestId` 到 `/api/v1/me/ai/customer-service/ask`；业务事实和用户范围由 Java 读取和组装。Admin AI 走独立 Operator 路径。
- Rate limit：响应 headers 和 429 body 来自 Java/Redis 运行时。

### Mock / Fallback / Demo

- AI Provider 是确定性的 `commerceflow-mock`，不调用外部模型；页面必须显示 `MOCK` 和“未调用外部大模型”。
- FastAPI 或 Provider 不可用时使用 `java-fact-fallback`，必须显示 fallback，而不能静默替换真实错误。
- `.env.example` 中的 Demo 用户/Operator 只在显式本地 Demo 模式可用，不是生产登录或权限系统；无身份配置时前后端显示 401/Operator 状态而不回填 Mock 数据。
- 订单、AI Trace 和限流冒烟会写入本地合成数据，不代表真实客户或商业交易。
- `CREATED` 不得被表述成支付成功、已发货或物流完成。

## 3. 当前视觉和交互问题

### H5

1. 商品详情已能完成选择和加购，但商品图、价格、选中 SKU 和主购买动作的视觉权重还不够像成熟 Storefront，详情信息偏“卡片堆叠”。
2. SKU 选择是纵向列表，颜色/尺码/库存之间的关系可以更快扫描；缺货状态需要在选项本身和底部购买栏同时表达。
3. 购物车、确认订单、结果和详情页各自有局部样式，品牌 header、金额摘要、状态反馈和底部导航缺少统一节奏。
4. AI 页面证据内容完整，但正常买家路径和开发证据路径的层级还可以拉开：先回答，再折叠 Business Facts / Evidence / Trace。
5. 页面需要统一 `prefers-reduced-motion`、键盘 focus、按钮 loading 和图片失败语义，并保持 390px 无横向溢出。

2026-09-03 的 `EC-UI-02` 已补齐商品列表的本地发现层：搜索会在当前真实 `GET /api/products` 响应内匹配商品名、描述、分类和编号；分类选项从真实响应动态生成；排序只改变前端展示顺序；无匹配显示明确空状态。该层没有新增搜索 API，也没有把本地筛选结果伪装成后端查询。

### Admin

1. 公共壳层已有深色侧栏，但页面内容仍接近“侧栏 + 标题 + 卡片/表格”模板；需要更明确的产品工作台上下文和运行边界。
2. 运营总览的指标卡容易被误读成商业 KPI，需要强化“本地事实源”和“非商业收入”语义，让低库存、订单和 AI Trace 成为可操作的信号。
3. Product/SKU 页的真实字段齐全，但产品选择、当前 SKU、库存解释和只读边界可以分成更清晰的视觉区域。
4. Order Evidence 当前技术字段较多，移动和窄桌面阅读成本高；应采用“订单摘要 → 商品快照 → 库存事件 → 幂等/事务说明”的证据阅读顺序，减少超宽表格依赖。
5. AI 工作台三栏结构正确，但需要更明显地区分 SKU context、对话主任务和 Evidence/Trace 旁证，避免右侧成为信息墙。

## 4. 组件处理决策

### 可以保留

- `ProductImage.vue`：已经提供真实图片、空图和加载失败占位语义。
- `MobileNotice.vue`：保留为状态/边界提示基础组件。
- API helper、类型、整数金额 helper、限流/库存 policy helper。
- 当前页面的 `data-testid`、真实 API 调用和已有状态测试契约。
- 现有商品素材和截图 provenance，不新增第三方 Logo、图片或字体。

### 需要重构

- `MobileHeader.vue`：重构成更稳定的品牌 topbar，支持 back、action、route context 和无障碍 label。
- Admin `App.vue` 和 `style.css`：重构为“深色导航 + 单层 context bar + 领域内容”的 shell，建立 token、focus、motion 和窄屏规则。
- H5 商品详情、购物车、订单结果/详情和 AI 页面：统一 spacing、金额、状态和固定动作层级。
- Admin 四个页面：保留数据逻辑，重做页面结构、标题区、summary、evidence 和状态面板。

### 需要新建

- Admin：`components/AdminStatusPill.vue`、`components/AdminSectionHeader.vue`、`components/AdminBoundaryStrip.vue`，集中处理状态、区块标题和 Showcase 边界。
- H5：`components/MobileBottomNav.vue`、`components/MobileStatusPill.vue`、`components/MobileMoney.vue`，集中处理导航、库存/订单状态和 CNY 金额。
- `docs/design/FRONTEND_PAGE_SPECS.md`：逐页记录布局、字段、状态和截图验收标准。

## 5. 状态审查

所有页面都必须保留以下正式状态，不得用 fixture 代替真实 API 错误：

| 页面 | Loading | Empty | Error / Retry | Success | Disabled |
| --- | --- | --- | --- | --- | --- |
| 商品列表/详情 | API skeleton 或读取提示 | 无商品/无详情 | API 错误 + 重试 | 商品/选择结果 | 下架、无库存 SKU |
| 购物车/订单确认 | 读取/更新/提交中 | 空购物车 | 网络/库存/幂等冲突 + 重试 | 更新成功、订单创建 | 更新中、库存上限 |
| 订单结果/详情 | 读取订单 | 缺少订单号不伪造 | 订单不存在 + 重试 | `CREATED` 结果 | 只读状态 |
| AI 客服 | 读取事实、发送中 | 尚未发送回答 | 429、Provider error、网络错误 | Answered + Evidence/Trace | 限流倒计时、发送中 |
| Admin 列表/详情 | 请求 skeleton | 无匹配真实数据 | API 错误 + 重试 | 选中详情、证据读取 | 只读、请求中 |

## 6. 不变的 API 和后端行为

- 不改变 `GET /api/products`、`GET /api/products/{productId}`、购物车、订单和订单证据字段含义；消费者新链路使用 `/api/v1/me/...`，跨用户管理读使用 `/api/v1/operator/...`。
- 不改变订单幂等键、重复 SKU 聚合、CNY-only、库存条件扣减、事务回滚和库存流水逻辑。
- 不把前端 SKU 选择当成库存权威；提交时仍由 Java 再次校验。
- 不在前端保存或暴露真实 Provider API Key。
- 不新增支付、物流、退款、地址、生产认证、AI 历史或 Product/SKU 写 API；Phase 0-B 的身份适配器仍只是显式本地/测试 Showcase 边界。

## 7. 当前测试和截图基线

### Admin

```powershell
Set-Location apps/admin-web
npm test
npm run build
```

### Mobile

```powershell
Set-Location apps/mobile-app
npm test
npm run build
npm run build:uni
```

### Showcase / Browser

```powershell
powershell -NoProfile -File .\scripts\showcase\start.ps1 -IncludeMobile
powershell -NoProfile -File .\scripts\showcase\status.ps1
powershell -NoProfile -File .\scripts\showcase\verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke
powershell -NoProfile -File .\scripts\showcase\stop.ps1
```

验收截图必须来自真实运行页面：

- Admin：`1366×768` 或更宽桌面截图，重点覆盖 Overview、Product/SKU、Order Evidence、AI Workbench。
- H5：`390×844` 截图，重点覆盖 Product Detail、Cart/Checkout/Result、AI。
- 每张图检查文字、空白、破图、横向溢出、控制台错误和 Demo/Mock/Fallback 标识。

## 8. 本轮边界说明

- Admin：`App.vue`、`style.css`、四个领域页面、新的基础组件目录和必要的页面测试选择器。
- Mobile：`App.vue`、`theme.css`、`MobileHeader.vue`、新基础组件、七个页面的模板/样式；API 和 policy helper 原则上不改。
- 文档：本审查、页面规格、README 的截图/运行说明、当前验证记录和前端交付记录。
- 本轮 Phase 0-B 只对 Java 增加服务器派生的消费者/Operator 路由与策略，对前端做请求封装、状态文案和契约测试收口；不修改 FastAPI Provider 逻辑、另一个项目或无关业务。
