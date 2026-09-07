# CommerceFlow 前端页面规格

版本：v2 frontend redesign  
日期：2026-09-02  
产品方向：H5 是浅色品牌电商；Admin 是证据优先的运营台。所有页面都以真实 API 字段为内容源。

## 0. 统一设计系统

### 视觉方向

- H5：quiet gallery storefront。暖白/浅灰画布、大面积真实商品图、墨色正文、电光蓝购买动作，AI 入口是辅助动作。
- Admin：evidence-first operations cockpit。深海军导航、雾灰工作区、青色健康状态、橙色库存风险、蓝色主操作；技术 ID 使用等宽字体。
- 与普通后台的区别：H5 围绕“选择可购买 SKU”推进；Admin 围绕“读取真实事实并检查执行证据”推进，不使用无来源的增长、支付或物流指标。

### Token

```css
/* Admin */
--admin-ink: #14233a;
--admin-canvas: #f3f6fa;
--admin-sidebar: #08162c;
--admin-panel: #ffffff;
--admin-line: #dde5ef;
--admin-blue: #2875ee;
--admin-teal: #0f9f90;
--admin-amber: #c47a20;
--admin-red: #d95363;

/* H5 */
--mall-canvas: #f7f8fa;
--mall-surface: #ffffff;
--mall-ink: #151b27;
--mall-muted: #697587;
--mall-primary: #145ff2;
--mall-line: #e4e8ee;
--mall-success: #159a70;
--mall-warning: #d68726;
--mall-danger: #c94f5a;
```

### 公共状态规则

- 状态必须同时有文字和结构/图标差异，不能只靠颜色。
- 所有按钮提供 hover、focus-visible、active、disabled 和 loading 反馈。
- 异步操作显示真实请求状态；错误不自动换成 Demo 数据。
- 动画只使用 opacity/transform，且在 `prefers-reduced-motion: reduce` 时关闭。
- 图片统一使用本地真实素材，失败时显示保留商品文字的 placeholder。

## 1. H5 商品列表 `/pages/index/index`

### 页面目标 / 用户 / 主任务

让演示者在 10 秒内理解这是一个真实商品入口，并进入一个可选择 SKU 的商品详情。目标用户是消费者路径的演示者；主任务是浏览商品、看价格和进入详情。

### 参考案例和借鉴点

- Apple Store：商品图优先、价格层级和品牌感。
- Vercel Shop：轻量商品卡和明确的 Add-to-detail 路径。
- Shopify Products：Product → SKU 数量的语义，但不引入未实现的编辑/营销能力。

### 布局和组件

```text
Brand topbar
  ├── CommerceFlow wordmark
  ├── Local Showcase / Demo Buyer status
  └── Cart / Orders navigation
Intro band
  ├── 商品目录
  └── real API source note
Product feed
  └── image-led product card
Discovery layer
  ├── local search over the loaded real response
  ├── response-derived category chips
  └── local sort cycle with result count
Bottom navigation
  ├── 商品
  ├── 购物车
  └── 订单
```

保留 `ProductImage`，新增 `MobileBottomNav`；商品卡使用真实 `name`、`description`、`categoryName`、`productCode`、SKU 数量、最低 `salePrice` 和 `coverImagePath`。

`EC-UI-02` 增加 `mobile-catalog-policy.mjs` 作为纯函数策略层：搜索覆盖商品名、描述、分类和编号，分类只从当前 API 返回的数据去重生成，排序支持推荐、价格升序、价格降序和名称；结果数量和无匹配状态必须明确。该能力是当前已载入真实商品的前端发现，不宣称服务端分页、全文检索或营销推荐。

### 状态和边界

- Loading：列表骨架或“正在读取真实商品”。
- Empty：显示“没有可展示的真实商品”，不添加 fixture。
- Error：显示 API message 和“重新加载”。
- Success：点击卡片进入详情；图片破损只替换图片区域。
- Demo：显示 `LOCAL SHOWCASE / userId=1`，不显示支付/配送承诺。

### Desktop / 390px

- H5 桌面预览：内容最大宽度 760px，双列商品卡。
- 390px：单列卡片，图片 112–128px，标题不截断，操作区不溢出。

### 可访问性 / 截图标准

- 商品卡使用 button 语义或可聚焦容器，图片有商品 alt。
- 390×844 截图首屏能看到品牌、商品图、价格、状态和底部导航；无横向滚动、无破图。

## 2. H5 Product Detail `/pages/product/detail`

### 页面目标 / 用户 / 主任务

让用户在 10 秒内理解商品、选定完整 SKU、确认真实库存并完成“加入购物车”或“查看购物车”。

### 参考案例和借鉴点

- Apple Store：首屏商品图和购买信息优先。
- Vercel Shop：商品信息、变体和动作分区清楚。
- Shopify Variants：SKU 选择和库存语义明确。
- Medusa Starter：Gallery、Product Info、Variant Selector、Actions 分层。

### 布局和组件

```text
Topbar: back / CommerceFlow / cart
Hero gallery: selected SKU image + product identity
Purchase summary: price / CNY / sale status / product code
Variant section: color + size-aware SKU choices
Selected SKU rail: SKU code / price / stock
Inventory facts: Java source / checkout revalidation / AI read-only
AI assistant entry: secondary action
Sticky purchase bar: cart / add-to-cart
```

领域组件：`ProductImage`、`MobileStatusPill`、`MobileMoney`、`AiAssistantEntry`（当前以页面内组件实现，可后续抽离）。真实字段：`product.name`、`description`、`status`、`coverImagePath`、SKU `color`、`size`、`skuCode`、`salePrice`、`currency`、`availableStock`。

### 状态和边界

- Loading / Not Found：不显示默认商品；提示真实请求状态和重试。
- Success：默认选择第一件可购买 SKU，否则选择第一件真实 SKU。
- Disabled：零库存 SKU 不能成为购买目标；下架商品所有购买按钮 disabled。
- Action loading：加入购物车按钮显示加入中，阻止重复点击。
- Action success/error：使用 `MobileNotice`，保留真实 API 错误。
- AI：只作为辅助入口，AI 不修改库存；显示 Mock/事实边界。

### Desktop / 390px

- 桌面预览：左侧大图、右侧购买信息；规格区和库存事实在下方。
- 390px：商品图占据第一视觉区；价格和状态紧随其后；SKU 选择用可扫描的两列/单列组合；底部购买栏固定且为安全区留出空间。

### 可访问性 / 截图标准

- 每个 SKU 选项可聚焦，选中状态同时有边框、文字和 `aria-pressed`。
- 缺货选项有“缺货”文字和 disabled 语义。
- 截图必须看到商品图、CNY、当前 SKU、库存、AI 辅助入口和固定购买栏。

## 3. H5 Cart `/pages/cart/index`

### 页面目标 / 用户 / 主任务

把服务端购物车变成可核对的商品清单，支持数量调整、删除和进入结算。

### 参考案例和借鉴点

- Vercel Shop：购物车反馈即时、内容轻量。
- Shopify：商品变体、单价、数量和小计的层级。

### 布局和组件

```text
Topbar: back/brand + continue shopping
Cart summary: item count + local data boundary
Cart line: image / product / SKU / spec / unit price / quantity / subtotal
Checkout dock: total CNY + item count + confirm action
Page navigation: 结算栏优先承载继续购买；商品列表、订单和 AI 页面使用底部主导航
```

### 状态和边界

- Loading、empty、error/retry 保留。
- 数量加到 `availableStock` 时禁用加号；更新请求中禁用该行。
- 删除成功后允许进入 empty 状态。
- 金额使用整数分 helper；不展示运费、优惠、支付或配送。

### 390px 验收

商品图片不挤压名称和 SKU；数量控件、删除和小计可同时理解；固定结算栏不遮挡最后一项。

## 4. H5 Order Confirm / Result / Detail / List

### 页面目标

以最少步骤完成真实订单提交，并把服务端 `CREATED` 结果和快照展示清楚。

### 布局

```text
Confirm: source cart reload -> item snapshot -> CNY total -> create order
Result: created marker -> orderNo -> amount -> status -> detail action
Detail: order identity -> item snapshots -> amount -> CREATED boundary
List: order cards -> orderNo / time / item count / amount -> detail
```

### 参考案例

- Shopify Orders：订单摘要、商品快照、状态层级。
- Stripe Dashboard：详情优先、技术元数据和事件可展开。

### 边界和状态

- `Idempotency-Key` 由现有 helper 生成并原样传给 Java。
- 409、库存不足、网络错误都必须展示错误；不能显示“支付成功”。
- 订单详情使用服务端快照，不从当前商品接口回填历史信息。
- 只有 `CREATED / 已创建`，不加入物流、支付和退款状态。

### 截图标准

订单结果截图必须包含真实 `orderNo`、金额、CNY、`CREATED` 和“非支付/发货完成”说明；订单详情截图必须包含商品图片或明确的图片失败占位。

## 5. H5 AI Customer Service `/pages/ai/customer-service`

### 页面目标

让消费者先得到一个清晰的商品事实回答，再按需展开开发证据。

### 参考案例和借鉴点

- Vercel/Apple：主任务简洁，辅助信息不抢主操作。
- Stripe：技术 metadata 可展开但不打断主阅读路径。

### 布局

```text
Topbar + selected product context
Provider / Redis request status
Quick questions
Conversation: user question -> answer -> provider/mode/latency
Evidence accordion
Trace accordion
Business Facts / developer boundary accordion
Fixed composer
```

### 状态

- loading：真实商品/SKU 读取。
- sending：输入和快捷问法 disabled。
- answered：显示回答、Provider、latency；Evidence/Trace 默认可展开。
- unsupported / insufficient context：按返回状态展示，不生成库存答案。
- 429：显示 Retry-After 倒计时和待重试问题。
- degraded：明确 `FAIL_OPEN`，不虚构配额。

## 6. Admin App Shell

### 页面目标 / 视觉方向

建立一个“证据优先”的运营壳层，而非普通 KPI 后台。

```text
Fixed dark sidebar
  ├── CommerceFlow mark
  ├── Workbench navigation
  └── LOCAL SHOWCASE boundary
Single context bar
  ├── current area
  ├── data source / provider mode
  └── demo user / runtime status
Main domain canvas
```

### 组件

- `AdminStatusPill`：状态文字、dot、tone 和 aria。
- `AdminSectionHeader`：eyebrow、标题、说明、右侧 action/metadata。
- `AdminBoundaryStrip`：LOCAL SHOWCASE / Mock / read-only 边界。

### 响应式 / 无障碍

- 1366px：侧栏、context bar、领域内容稳定排列。
- 960px 以下：内容改为单列，证据区按顺序阅读。
- 760px 以下：侧栏变为横向导航，所有页面不得横向溢出。
- `main` 有稳定标题，导航有 `aria-current`，按钮有 focus-visible。

## 7. Admin Operations Overview

### 目标

让运营者从真实数据源快速判断“商品、库存、订单、AI 运行边界发生了什么”。

### 布局

```text
Header: 运营总览 / refresh / generatedAt
Signal band: product / sku / stock / orders / AI interactions
Main split:
  left: recent created orders + low stock watchlist
  right: AI trace signal + runtime boundary
```

删除或不引入：支付 KPI、物流 KPI、转化率、虚构趋势、用户增长和活动数据。

### 状态和截图

- Loading、error/retry、empty order、empty low-stock 必须保留。
- 截图首屏能同时解释真实数据来源、本地边界和一条订单/库存信号。

## 8. Admin Product / SKU

### 目标

以 Product → SKU → Inventory 层级检查商品事实，当前为只读页。

### 布局

```text
Header + API source note
Toolbar: search / category / stock state
Master rail: product image / code / category / SKU count / stock sum
Detail stage: product hero + status + identifiers
SKU table/card: image / code / color / size / price / CNY / stock / status
Fact strip: Java source / no write action / checkout revalidation
```

### 状态和边界

- 列表和详情分别显示 loading/error/empty。
- 过滤无结果时不伪造商品。
- `ON_SALE`、库存正常/偏低/缺货使用统一状态组件。
- 不显示 create/edit/import/restock/payment/shipment 控件。

## 9. Admin Order Inventory Evidence

### 目标

让面试者按证据顺序看懂一次订单执行，而不是在一张超宽表里寻找技术字段。

### 布局

```text
Header + userId/order search
Order rail: orderNo / item preview / amount / CREATED / time
Evidence stage:
  1. order summary
  2. order item snapshot cards/table
  3. inventory movement event cards
  4. idempotency + transaction boundary
```

### 状态

- list/detail loading、empty、error/retry 保留。
- 图片失败时保留商品名称、SKU 和数量。
- movement 展示 `stockBefore`、`quantity`、`stockAfter`，并保留 `ORDER_DEDUCT`。
- 幂等键使用等宽字体并可换行；不能把 replay/conflict 伪装成订单列表记录。

### 截图标准

1366px 截图要同时看到左侧订单选择和右侧订单摘要/快照/库存证据；重点字段无需横向滚动才能读懂。

## 10. Admin AI Customer Service Workbench

### 目标

让用户一次看懂“选择了哪个 SKU、问了什么、Java 事实是什么、Provider 怎么回答、证据和 Trace 如何回溯”。

### 布局

```text
left: real SKU selector + search/filter + stock state
center: single-round conversation + composer + response status
right: provider/rate-limit + facts + Evidence + Trace
```

### 状态

- Product catalog loading/empty/error/retry。
- 未发送时右侧是当前选择预览和 Evidence/Trace empty state。
- 回答后显示实际 Provider、mode、fallback、latency、Evidence 数量和 Trace 步数。
- 429、unsupported、fallback、network error 均与 answered 视觉区分。

### 截图标准

1366px 截图要看到三栏关系；右侧技术信息可滚动但不能压缩到不可读；页面显示 `commerceflow-mock / MOCK` 和本地 Showcase 边界。

## 11. 交付截图清单

使用真实运行页面覆盖以下文件，只有完成浏览器检查后才更新 README 引用：

```text
screenshots/v2/01-dashboard-real.png
screenshots/v2/02-product-sku-real.png
screenshots/v2/03-order-inventory-real.png
screenshots/v2/04-ai-customer-service-real.png
screenshots/v2/07-mobile-product-detail-real.png
screenshots/v2/08-mobile-order-flow-real.png
```

每张图记录：页面、URL、viewport、数据来源、Provider/Mock 状态、是否有 console error、破图数量、横向溢出结果和生成时间。
