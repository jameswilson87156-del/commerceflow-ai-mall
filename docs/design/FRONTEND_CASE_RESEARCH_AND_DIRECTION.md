# CommerceFlow 前端案例筛选与视觉方向

版本：v1.0  
日期：2026-09-03  
适用范围：`apps/mobile-app`（消费者 H5）与 `apps/admin-web`（运营后台）  
当前状态：方向评审中；本文件完成前，不继续进行下一轮全站视觉编码

## 0. 先说结论

当前页面的问题不是“颜色还不够漂亮”，而是同时混用了几种互相竞争的语言：Apple 式留白、普通商城卡片、后台技术标签、营销型大标题和开发者证据字段。每个局部看起来都能解释，但放在同一屏以后没有一个明确的主任务，所以会出现“像做出来了，但不像一个成熟产品”的感觉。

本项目后续采用两套有边界的视觉方向：

1. **H5：Calm Product Storefront（克制的产品型商城）**
   - 借鉴 Apple Store 的商品叙事和购买层级。
   - 借鉴 Vercel Commerce、Saleor Storefront、Medusa Starter 的真实商品、变体、购物车和结算组织方式。
   - 不复制 Apple 的 Logo、文案、图形、页面源码或完整品牌识别。
   - 第一视觉只服务“看清商品 → 选择 SKU → 购买”，技术字段和 AI 证据放到次级层级。

2. **Admin：Evidence-first Operations Console（证据优先的运营控制台）**
   - 借鉴 Linear 的低噪声导航、信息层级和密度控制。
   - 借鉴 Stripe Dashboard 的摘要 → 详情 → 事件/证据阅读顺序。
   - 借鉴 Shopify Polaris 的状态、表单、筛选和运营组件语法。
   - AI 工作台再借鉴 Intercom 的“AI 回答 + 人工接管 + 质量分析”工作流，但不宣称项目已经具备 Intercom 的全量能力。

**推荐先做 H5 商品详情页，再扩展 H5 首页和购物车。** 用户当前正在查看的 `product/detail` 正好是最能判断方向的一页；方向没有通过前，不继续把订单、AI 和后台一起改掉。

## 1. 本轮案例筛选方法

本轮将案例分为两类：

- **视觉/交互案例**：看产品在真实场景中如何处理焦点、留白、密度、导航、购买动作和证据层级。
- **GitHub / 官方开源实现案例**：只看真实商城的产品模型、变体选择、购物车边界、页面拆分和可扩展性；不复制源码和组件。

筛选标准：

- 页面是否围绕真实用户任务，而不是只展示漂亮的卡片。
- 商品图、商品事实、SKU、库存和操作之间的层级是否清楚。
- 能否迁移到当前 Vue 3 + UniApp H5 和 Vue 3 Admin，而不强迫项目改技术栈。
- 是否能支持当前真实 API 边界：商品、SKU、库存、购物车、订单、AI 回答、Evidence 和 Trace。
- 是否能通过真实浏览器截图验收，而不是只凭设计稿或 AI 概念图判断。

参考页面只用于分析布局、密度和交互原则；不把参考页面当成自己的真实能力证明，也不直接复制第三方源码、Logo、图片、字体或品牌文案。

## 2. 入选案例与取舍

### 2.1 Apple Store：H5 的视觉锚点

- 官网：[Apple Store](https://www.apple.com/store)
- 参考价值：商品目录按产品类别组织，最新商品使用大面积商品图和简洁的购买信息；产品页面的核心不是后台字段，而是让用户快速理解“这是什么、多少钱、如何选择和购买”。
- 借鉴到 CommerceFlow：
  - 首屏只有一个主商品焦点，商品图片的面积和清晰度高于装饰性文案。
  - 商品名、价格、可购买状态、规格选择和主操作形成连续的购买路径。
  - 使用系统字体、稳定的留白和少量高对比动作色，避免每个区块都变成卡片。
  - 详情页先给购买决策，技术信息放在后面的“商品事实”区域。
- 不照搬：
  - 不使用 Apple Logo、Apple 品牌名、Apple 的产品文案和页面素材。
  - 不把我们的普通商品强行做成发布会式大 hero；当前商品目录的任务是购买，不是品牌宣传。
  - 不把 Apple 的黑白比例和蓝色数值机械复制为项目品牌。

### 2.2 Vercel Commerce：真实 storefront 的页面组织

- Live：[demo.vercel.store](https://demo.vercel.store)
- GitHub：[vercel/commerce](https://github.com/vercel/commerce)
- 参考价值：它把高性能 storefront、商品目录、商品详情、购物车和 provider 边界拆成清晰的页面与数据层；仓库说明也明确其目标是可替换 commerce provider 的 storefront 模板。
- 借鉴到 CommerceFlow：
  - 商品卡只展示完成浏览决策所必需的信息：图片、名称、价格、变体/库存提示和进入详情的动作。
  - 商品详情页把 gallery、购买信息、variant selector、cart action 分成不同层级。
  - 购物车是购买流程的一部分，不再是“另外一套卡片风格”。
  - 前端展示层和真实 API 数据适配层保持边界，继续保留当前 Java API 契约。
- 不照搬：
  - 当前项目不是 Next.js，不迁移 React Server Components、Next.js 路由或其代码结构。
  - 不因为参考了 Vercel Commerce 就声称项目拥有它的性能、支付或 provider 能力。

### 2.3 Saleor Storefront：商品变体和 headless commerce 边界

- Live：[Saleor Storefront](https://storefront.saleor.io/default-channel/pages/about)
- GitHub：[saleor/storefront](https://github.com/saleor/storefront)
- 参考价值：Saleor 的 storefront 将产品展示、变体选择、购物车和 checkout 作为可组合的前端体验，同时把 commerce API 和 storefront 解耦。
- 借鉴到 CommerceFlow：
  - 商品详情页要让用户先看到产品，再看到变体和购买动作；变体不是一长串技术字段。
  - 每个 SKU 选项需要有明确的可选、已选、缺货和不可用状态。
  - 商品历史快照、服务端库存和下单校验仍由后端作为权威，前端只负责表达。
  - 购物车/结算可以逐步增强，不需要把所有支付、配送、优惠能力伪造出来。
- 不照搬：
  - 不迁移 Next.js、GraphQL、Tailwind 或 Saleor 的组件实现。
  - 不因为页面有 checkout 结构就增加当前项目没有的支付、地址、物流能力。

### 2.4 Medusa Starter：购买流程的可扩展骨架

- Live：[Medusa Storefront Starter](https://next.medusajs.com/)
- 官方文档：[Storefront Development](https://docs.medusajs.com/learn/storefront-development)
- GitHub：[medusajs/nextjs-starter-medusa](https://github.com/medusajs/nextjs-starter-medusa)
- 参考价值：Medusa 官方文档明确 storefront 可以独立部署、使用自定义前端技术，并把商品读取和 commerce 操作通过 Store API 组织起来。
- 借鉴到 CommerceFlow：
  - 画廊、商品信息、规格、库存、购物车和订单确认应是连续流程，而不是每页重新发明一套视觉组件。
  - 用“产品 → SKU → 购物车行 → 订单快照”的对象关系组织界面。
  - 将未来真实模型接入点放在 API helper 和页面状态层，不把数据逻辑散落到视觉组件里。
- 不照搬：
  - 不把 Medusa 的默认 starter 当最终品牌视觉。
  - 不为了“像真实商城”而引入当前后端没有实现的用户账户、支付和物流流程。

### 2.5 Shopify Polaris：Admin 的运营组件语法

- 官方设计系统：[Shopify Polaris Getting Started](https://polaris.shopify.com/)
- 官方视觉指南：[Shopify App Visual Design](https://shopify.dev/docs/apps/design/visual-design)
- GitHub 参考：[Shopify Polaris React archive](https://github.com/Shopify/polaris-react-archive)
- 参考价值：Polaris 的定位是 Shopify Admin 的设计系统，而不是面向消费者 storefront 的品牌模板；它强调运营者在多个后台工作流中获得一致的控件、状态和信息反馈。
- 借鉴到 CommerceFlow Admin：
  - 统一状态 pill、筛选、表单、section header、空状态和错误重试。
  - 内容区块用清晰的标题、说明和操作组成，不用随机的圆角卡片堆满页面。
  - 运营页面先给当前对象和下一步动作，再给技术字段。
  - 风险、缺货、只读和本地 Showcase 等状态同时使用文字和结构表达。
- 不照搬：
  - 不把 Polaris 直接套到消费者 H5；那会让商城变成后台表单。
  - 不迁移 React 组件源码或强行切换到 React。

### 2.6 Linear：低噪声、高密度的工作台

- 官网：[Linear](https://linear.app/)
- 设计复盘：[A calmer interface for a product in motion](https://linear.app/now/behind-the-latest-design-refresh)
- 参考价值：Linear 的设计复盘强调让导航和辅助元素退后，把当前工作对象放在前景；它在信息密集的产品中控制视觉噪声，同时保留足够的内容密度。
- 借鉴到 CommerceFlow Admin：
  - 导航、环境、数据来源、用户身份等上下文信息保持稳定，但不与领域任务争夺注意力。
  - 订单证据、AI 回答和 Trace 使用主次层级，不把所有字段同时放大。
  - 工作台采用明确的左侧队列 / 中间对象 / 右侧辅助证据关系。
  - 通过细边框、低对比背景和少量选中色表达层级，而不是靠大量渐变、发光和胶囊标签。
- 不照搬：
  - 不复制 Linear 的 Logo、图标、快捷键文案和产品名称。
  - 不把商城 H5 设计成 issue tracker。

### 2.7 Stripe Dashboard / Apps：摘要到证据的阅读顺序

- 官方页面：[Stripe Apps](https://stripe.com/apps)
- Dashboard 说明：[Stripe Dashboard](https://support.stripe.com/topics/dashboard)
- 参考价值：Stripe 的运营界面围绕账户活动、产品、报告、事件和可操作详情组织；对 CommerceFlow 来说，最有价值的是“先看摘要，再钻取到事实和事件”的结构，而不是照搬金融业务。
- 借鉴到 CommerceFlow Admin：
  - Overview 只展示项目真实存在的商品、库存、订单、AI Trace 和运行边界信号。
  - Order Evidence 按“订单摘要 → 商品快照 → 库存 movement → 幂等/事务说明”阅读。
  - AI 页面按“回答 → Provider/限流状态 → Business Facts → Evidence → Trace”阅读。
  - 技术 ID、时间、幂等键和 JSON 使用等宽字体，但默认不让它们压过主任务。
- 不照搬：
  - 不显示支付金额、MRR、退款、争议或金融合规等当前项目没有的指标。
  - `CREATED` 仍只表示订单已创建，不改写成支付成功或已发货。

### 2.8 Intercom / Fin：AI 与人工协作的工作流参照

- 官方产品说明：[Intercom — Fin and Intercom](https://www.intercom.com/help/en/articles/591233-our-products-explained)
- 参考价值：Intercom 将 AI Agent 的训练、测试、部署、分析与人工工作台放在同一运营体系中；这个案例适合借鉴 AI 回答、人工接管和质量反馈的关系，不适合拿来伪造本项目的产品范围。
- 借鉴到 CommerceFlow：
  - H5 AI 先给买家一个清晰的商品事实回答，Evidence/Trace 默认折叠。
  - Admin AI 工作台让运营者看到选择的 SKU、问题、回答、事实来源和 Trace，并保留 fallback/Mock 边界。
  - 当回答不足、限流或需要人工处理时，状态应该成为明确的下一步，而不是一个模糊的“AI 出错”。
- 不照搬：
  - 不宣称当前项目已经有全渠道 Inbox、Fin Agent 训练平台或完整客服工单体系。
  - 不把技术证据全部塞进消费者首屏。

## 3. 最终视觉方向

### 3.1 H5：Calm Product Storefront

一句话定义：**像一个认真做商品展示和购买决策的品牌商城，而不是“后台数据搬到手机上”，也不是 Apple 官网的复制品。**

#### 视觉规则

| 角色 | 方向 |
| --- | --- |
| 画布 | 温和的近白/浅灰，不使用渐变背景和大面积装饰 |
| 商品舞台 | 白色或极浅色，商品图为第一视觉，允许大面积留白 |
| 正文 | 近黑色，中文优先；商品名称和价格承担主要层级 |
| 主动作 | 单一蓝色主动作；同一视口最多一个最强购买 CTA |
| 辅助动作 | 线框、文字链接或低对比按钮，不与购买动作争夺注意力 |
| 状态 | 绿色/橙色/红色配文字，不能只靠颜色表达库存和订单状态 |
| 边框 | 细、低对比；不让每一个区块都拥有厚重圆角卡片背景 |
| 圆角 | 商品舞台可有中等圆角；规格选项和列表使用更克制的边界 |
| 字体 | 系统字体：`-apple-system, BlinkMacSystemFont, "SF Pro Display", "PingFang SC", "Microsoft YaHei", sans-serif` |
| 动效 | 只做图片/区块的轻微 opacity/transform；支持 reduced motion |

#### H5 页面配方

**首页 `/pages/index/index`**

```text
极简品牌栏：品牌 / 购物车 / 订单
        ↓
商品导览：一句很短的定位，不做大段营销宣言
        ↓
主商品舞台：真实商品图 + 名称 + 起售价 + 在售状态
        ↓
商品列表：统一的图片、名称、价格、SKU/库存提示
        ↓
底部导航：商品 / 订单 / AI 辅助
```

首页的重点是“商品被看见”。当前 `AI MALL`、技术运行状态和开发边界不应与主商品标题抢首屏；这些内容可以保留为轻量状态入口或二级信息。

**商品详情 `/pages/product/detail`（下一轮第一张要做对的页面）**

```text
顶部：返回 / 品牌上下文 / 购物车
        ↓
大商品舞台：当前 SKU 图片，保持稳定比例和足够留白
        ↓
购买信息：商品名 / 一句描述 / CNY 价格 / 在售状态
        ↓
规格选择：颜色、尺码等真实 SKU 选项；已选、缺货、不可选一眼可区分
        ↓
商品事实：库存、SKU code、Java source、结算时再次校验（次级区域）
        ↓
AI 辅助入口：问商品，不抢购买主任务
        ↓
固定购买栏：购物车 / 加入购物车或立即购买
```

必须做的减法：

- 首屏不显示 `PRODUCT / PROD-1001`、`4 VARIANTS` 这类开发者标签作为主要标题。
- 不使用大段全大写英文作为装饰性 section title。
- 不同时把“加入购物车”和“立即购买”做成两个同权重的高饱和蓝色按钮。
- 不让规格选项变成一排难以扫描的小卡片；颜色/尺码要按真实维度分组。
- 底部购买栏只固定必要动作，并为 H5 安全区留出空间。

**购物车、订单和 AI**

- 购物车沿用详情页的图片、名称、SKU、数量和金额节奏，不重新发明一套视觉语言。
- 订单确认只展示真实购物车快照、CNY 金额和“创建订单”；没有地址、支付、配送时，不增加假表单。
- 订单结果明确展示 `CREATED / 已创建`，不写“支付成功”“已发货”。
- H5 AI 先呈现买家能读懂的回答，再折叠 Business Facts、Evidence 和 Trace；Mock、fallback、限流和错误必须可见但不喧宾夺主。

### 3.2 Admin：Evidence-first Operations Console

一句话定义：**让面试官一眼看到这是一个能读真实数据、检查执行证据和理解 AI 边界的运营台，而不是普通的“侧栏 + 五张 KPI 卡 + 表格”模板。**

#### 视觉规则

| 角色 | 方向 |
| --- | --- |
| 壳层 | 深色石墨导航 + 低对比工作区；深色只承担导航和环境，不把内容做成发光 AI 大屏 |
| 工作区 | 雾灰/深灰的稳定画布，面板使用一到两级表面差异 |
| 主色 | 蓝色只用于选中、链接和主操作；青色表示健康，橙色表示风险，红色表示失败 |
| 密度 | 高于 H5，但每屏只突出一个主工作流 |
| 技术字段 | ID、时间、Trace、幂等键和 JSON 使用等宽字体，默认折叠次级内容 |
| 导航 | 一层 sidebar + 一层 context bar；不叠加重复状态条 |
| 卡片 | 以 section、divider、detail panel 和 timeline 为主，不用无意义的圆角卡片网格 |

#### Admin 页面配方

- `overview`：真实运行信号 + 最近订单 + 低库存 watchlist + AI Trace 状态；删除虚构增长和支付 KPI。
- `products`：Product → SKU → Inventory 主从关系；先看当前商品，再看 SKU 事实和库存状态。
- `orders`：左侧订单选择，右侧按证据顺序读订单摘要、商品快照、库存 movement、幂等/事务边界。
- `ai`：左侧真实 SKU 上下文，中间单轮问答和主动作，右侧 Provider/限流/事实/Evidence/Trace。

## 4. 设计系统初稿

### H5 tokens

```css
:root {
  --mall-canvas: #f5f5f7;
  --mall-surface: #ffffff;
  --mall-surface-muted: #fbfbfd;
  --mall-ink: #1d1d1f;
  --mall-muted: #6e6e73;
  --mall-line: #d2d2d7;
  --mall-primary: #0071e3;
  --mall-primary-pressed: #0066cc;
  --mall-success: #1d8348;
  --mall-warning: #a65f00;
  --mall-danger: #c9343f;
  --mall-radius-stage: 24px;
  --mall-radius-control: 999px;
  --mall-content-max: 1120px;
}
```

说明：这里借鉴的是 Apple 这类成熟产品站的层级和克制，不要求项目使用 Apple 的具体颜色、Logo 或字体文件。中文优先使用系统可用字体，不能为了“像 Apple”引入不可验证的商业字体。

### Admin tokens

```css
:root {
  --admin-canvas: #0f1319;
  --admin-sidebar: #0a0d12;
  --admin-panel: #171c24;
  --admin-panel-strong: #1d2430;
  --admin-line: rgba(182, 198, 220, 0.16);
  --admin-ink: #edf2f8;
  --admin-muted: #91a0b3;
  --admin-blue: #5d8dff;
  --admin-cyan: #2ac7c9;
  --admin-green: #39d98a;
  --admin-amber: #f2b35d;
  --admin-red: #ff6b7f;
  --admin-radius: 8px;
}
```

Admin tokens 只作为下一阶段设计基线；本轮不因为写入方向文档就改动 Admin 代码。

## 5. 当前页面的具体判断

结合当前 H5 商品首页和 `productId=102` 商品详情截图，当前版本可以保留的不是整套视觉，而是以下业务结构：

- 真实商品图、商品名、价格、SKU、库存和真实 API 状态。
- 规格选择、加入购物车、立即购买和 AI 辅助入口。
- Local Demo / Real API 的边界提示。
- 当前已有的 loading、error、empty、disabled、action loading 语义。

需要推倒重做的主要是视觉编排：

- 品牌栏、商品标题、英文技术标签和状态 pill 之间的层级。
- 商品舞台尺寸、图片裁切和首屏留白。
- 规格选择的分组方式和已选/缺货状态。
- 固定购买栏的高度、按钮权重和与页面内容的关系。
- H5 的“消费者内容”和“开发证据”边界。

因此，下一轮不是继续给现有页面加 CSS，而是**以真实 API 和现有 data-testid 不变为前提，重排 `product/detail` 的模板层级和样式结构**，通过一张 390×844 的浏览器截图先验收方向。

## 6. 下一步实施顺序

### 第 0 步：方向确认

只需要确认一句：

> H5 采用“Apple 产品购买层级 + 独立品牌视觉”的 Calm Product Storefront；Admin 采用“Linear × Stripe × Polaris”的 Evidence-first Operations Console。

确认后才进入编码。

### 第 1 步：只做 H5 商品详情

- 保留 API helper、类型、真实 SKU 选择、库存校验、加购和购买逻辑。
- 重做模板层级和 CSS，不先改首页、购物车、订单和 AI。
- 首屏只验收商品图、名称、价格、规格、库存、主动作和返回/购物车入口。
- 生成真实浏览器截图，检查 390×844、桌面预览、横向溢出、破图、控制台错误和可访问焦点。

### 第 2 步：验收通过后扩展 H5

顺序：商品首页 → 购物车 → 订单确认/结果/详情 → H5 AI。每一轮只改一个页面或一个紧密的页面闭环，不同时改全站。

### 第 3 步：单独重做 Admin

顺序：运营总览 → 商品与 SKU → 订单证据 → AI 工作台。Admin 不套用 H5 的 Apple 商城风格，而是单独按证据工作流验收。

## 7. 页面验收 Gate

商品详情页达到以下条件才进入下一页：

- 第一眼明确是商品详情和购买页，不像后台、技术演示或模板样例。
- 390×844 首屏能读到真实商品图、商品名、CNY 价格、在售/缺货状态和一个明确主动作。
- 商品图片是第一视觉，不被大段 slogan、技术标签或状态条压住。
- 规格按真实维度分组，已选、可选、缺货、禁用状态不用猜。
- 页面不依赖大量圆角卡片、渐变、发光、胶囊标签或无意义的大写英文来制造“高级感”。
- 真实 API 错误不会静默变成 Demo 数据；Demo/Mock/fallback 边界仍清晰。
- 无横向溢出、无破图、无关键中文截断、无控制台 error；按钮有 loading、disabled、focus-visible 和 reduced-motion 处理。
- 截图来自真实运行页面，不用 AI 概念图冒充验收证据。

## 8. 本轮交付和边界

- 本轮已经完成案例调研、GitHub 仓库筛选、视觉取舍和页面实施顺序。
- 本轮只新增这份方向文档，没有继续修改 H5 或 Admin 代码，也没有改变 Java、数据库、API、订单和 AI 逻辑。
- 当前本地页面可以继续打开查看，但它仍是上一轮的中间版本，不能作为最终视觉方向的验收通过依据。
- 真实 Provider、支付、物流、生产登录、公网部署和域名解析不属于本次视觉方向确认；这些应在页面方向通过后单独安排。

## 9. 进一步筛选：视觉案例和源码案例不能混为一谈

这次又补看了几个更偏产品审美的真实站点，并对 GitHub 搜索结果做了质量筛选。得到一个很重要的判断：

> 没有哪个 GitHub 仓库可以不加判断地直接变成 CommerceFlow 的最终前端。视觉参考要选成熟产品站，源码参考要选数据模型和购买流程清楚的仓库。

### 9.1 视觉案例的最后取舍

| 案例 | 适合借鉴 | 为什么不作为唯一方向 |
| --- | --- | --- |
| [Apple Store](https://www.apple.com/store) | 商品聚焦、购买层级、留白、规格和价格组织 | 品牌识别太强，完整照搬会像仿站，也不适合把普通商品做成发布会页面 |
| [Nothing Product](https://us.nothing.tech/products/phone-3?Capacity=12+256GB&Colour=Black) | 产品叙事、编号化内容、强烈的图文节奏 | 更适合单品发布，不适合当前多商品、多 SKU 的普通商城 |
| [Teenage Engineering Store](https://teenage.engineering/store) | 产品图片、极简目录、强烈的工业设计个性 | 视觉个性过强，照搬后会掩盖 CommerceFlow 自己的品牌和业务 |
| [Linear](https://linear.app/) | 低噪声、层级、密度、上下文导航 | 它是软件工作台，不是消费者商城 |

因此本项目的**唯一主视觉锚点选择 Apple Store 的购买层级**，而不是 Apple 的品牌外观；Nothing 和 Teenage Engineering 只作为“如何让商品更有性格”的观察样本，不直接成为页面皮肤。

### 9.2 GitHub 源码案例的最后取舍

优先保留以下仓库作为代码阅读和结构参照：

1. **主源码基线：[vercel/commerce](https://github.com/vercel/commerce)**
   - 目录和页面职责清楚，覆盖商品目录、商品详情、购物车和 provider 适配边界。
   - 适合借鉴页面拆分和数据流，不适合直接迁移到当前 UniApp/Vue 代码。

2. **商品模型参照：[saleor/storefront](https://github.com/saleor/storefront)**
   - 适合研究产品、变体、购物车和 checkout 如何保持一致。
   - 只借鉴对象关系、页面顺序和状态表达，不迁移 Next.js/GraphQL/Tailwind 实现。

3. **购买流程参照：[medusajs/nextjs-starter-medusa](https://github.com/medusajs/nextjs-starter-medusa)**
   - 适合研究 gallery、variant、inventory、cart 和 order flow 的连续性。
   - 不把 starter 默认样式当成最终视觉。

4. **Vue 迁移辅助参照：[ecomplus/storefront](https://github.com/ecomplus/storefront)**
   - 因为当前项目使用 Vue/UniApp，可以用它观察 Vue storefront 的组件组织和 headless API 接入思路。
   - 它只是迁移辅助，不是本项目的审美基准，也不替代当前 Java API 契约。

本轮 GitHub 检索中还出现了一些名称带有 `premium`、`luxe`、`admin dashboard` 的低星 Demo。它们很多是一次性模板、图片驱动的概念页或重复的 Tailwind 卡片，不具备足够的产品验证信号；因此不选为主参考，也不建议直接 clone 后改名字写入简历。

## 10. 最终落地组合，不再继续摇摆

### H5

- **视觉主参考**：Apple Store 的商品购买层级。
- **源码主参考**：Vercel Commerce 的 storefront 页面职责。
- **商品/变体状态参考**：Saleor + Medusa。
- **项目自己的差异**：CommerceFlow 的真实 Java 商品/库存/购物车/订单 API，以及“AI 商品客服 + Evidence/Trace”辅助能力。

### Admin

- **视觉和密度参考**：Linear。
- **运营组件参考**：Shopify Polaris。
- **证据阅读参考**：Stripe Dashboard。
- **AI 协作参考**：Intercom Fin 的 AI 与人工协作关系。
- **项目自己的差异**：订单执行证据、库存 movement、幂等键、Java 事实源、Provider/Mock/Fallback/Trace 边界。

### 明确不做的事情

- 不再继续寻找“一个仓库解决所有视觉问题”。
- 不直接 clone 某个 React/Next 项目改成 CommerceFlow 的名字。
- 不把 Apple、Nothing、Teenage Engineering 三种个性混到同一张 H5 页面。
- 不为了视觉效果新增支付、物流、优惠、会员、全渠道客服或生产权限。
- 不在方向确认前修改首页、商品详情、购物车、订单、AI 和 Admin 全站。

## 11. 给下一轮编码的单一任务

只做：`apps/mobile-app/src/pages/product/detail.vue`。

目标不是“把页面做得更花”，而是完成以下可验证结果：

```text
真实商品图成为第一视觉
    ↓
商品名 / 价格 / 在售状态成为第一信息层级
    ↓
颜色 / 尺码 / SKU 选择清楚可扫
    ↓
库存和技术事实放在次级区域
    ↓
加入购物车 / 立即购买有明确主次
```

完成后只生成一张真实 `390×844` 浏览器截图，先让用户判断“这是不是正确方向”；通过后再进入首页和购物车。这样可以把“审美方向错误”和“实现细节问题”分开处理。
