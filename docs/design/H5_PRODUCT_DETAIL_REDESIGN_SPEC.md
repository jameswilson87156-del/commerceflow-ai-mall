# CommerceFlow H5 商品详情页重做规格表

版本：v1.0  
日期：2026-09-03  
页面：`/pages/product/detail`  
实现文件：`apps/mobile-app/src/pages/product/detail.vue`  
设计基线：[前端案例筛选与视觉方向](./FRONTEND_CASE_RESEARCH_AND_DIRECTION.md)

## 1. 本页唯一目标

让用户在一个手机屏幕内完成：

```text
看清商品 → 看懂价格和库存 → 选择真实 SKU → 加购或立即购买
```

这不是营销落地页、后台页面或开发者 Trace 页面。AI 商品助手和 Java/库存事实必须保留，但只作为购买决策的辅助信息，不能抢商品主视觉。

## 2. 页面方向

**Calm Product Storefront：Apple 级别的购买信息层级 + CommerceFlow 自己的品牌视觉。**

视觉参考和代码参考分开使用：

- 视觉层级参考 [Apple Store](https://www.apple.com/store)，只借鉴商品焦点、留白、价格和购买路径。
- Storefront 结构参考 [Vercel Commerce](https://github.com/vercel/commerce)，只借鉴页面职责和商品/购物车组织。
- SKU 状态参考 [Saleor Storefront](https://github.com/saleor/storefront) 与 [Medusa Starter](https://github.com/medusajs/nextjs-starter-medusa)。
- 不复制第三方 Logo、文案、图片、字体、源码和完整品牌识别。

## 3. 模块编排表

| 顺序 | 模块 | 用户看到的内容 | 真实数据/行为 | 视觉规则 |
| --- | --- | --- | --- | --- |
| 1 | Detail navigation | 返回、CommerceFlow、购物袋入口 | `uni.navigateBack()`、进入 `/pages/cart/index` | 轻量 top bar；不再使用大段英文 breadcrumb |
| 2 | Product stage | 当前 SKU 或商品封面大图 | `selectedSku.imagePath || product.coverImagePath`、`ProductImage` 失败占位 | 商品图是第一视觉；白色舞台、稳定比例、无装饰性标签覆盖主体 |
| 3 | Product identity | 分类、在售状态、商品名、短描述 | `categoryName`、`status`、`name`、`description` | 中文优先；商品名和价格优先于 product code |
| 4 | Price row | `¥` 金额、`CNY`、库存语义 | `salePrice`、`currency`、`getStockPresentation` | 价格明确但不做巨大海报字；库存同时有文字和状态色 |
| 5 | Variant selector | 颜色、尺码、SKU、库存状态 | `product.skus`、`chooseSku`、`isPurchasableSku` | 采用可扫描的行式选择；已选有边框和勾选，缺货 disabled 且有“缺货”文字 |
| 6 | Selection summary | 已选颜色/尺码、SKU、当前库存 | `selectedSku`、`selectedStock` | 用一条事实摘要确认选择，不再重复堆卡片 |
| 7 | Product facts | 库存来源、下单校验、SKU 等开发事实 | 现有真实 Java API 说明 | 次级信息，可折叠；默认不压过购买路径 |
| 8 | AI entry | “询问 AI 商品助手” | `openAiCustomerService()` | 文字/线框辅助动作，不使用高饱和主按钮 |
| 9 | Action feedback | 加购成功、加购失败、购买失败 | 现有 `MobileNotice`、`ApiError` | 贴近操作结果显示；不能静默替换为 Demo |
| 10 | Sticky purchase bar | 购物袋、加入购物车、立即购买 | `addToCart()`、`buyNow()` | 白色半透明底栏；一个蓝色主动作，一个线框次动作 |

## 4. 字段映射表

| UI 文案 | 字段/来源 | 规则 |
| --- | --- | --- |
| 商品名 | `product.name` | 原样展示，允许自然换行 |
| 商品描述 | `product.description` | 只展示真实返回内容，不补写营销卖点 |
| 分类 | `product.categoryName` | 作为弱层级标签 |
| 商品状态 | `product.status` | `ON_SALE` 显示“在售”，其他状态保留真实语义 |
| 商品编号 | `product.productCode` | 放入事实区或辅助信息，不占首屏主标题 |
| 价格 | `selectedSku.salePrice` | 通过 `MobileMoney` 展示，保留 CNY |
| 货币 | `selectedSku.currency` | 不改写成美元或其他货币 |
| 规格 | `sku.color`、`sku.size` | 每个真实 SKU 一个可选项，颜色/尺码同时展示 |
| SKU 编号 | `sku.skuCode` | 等宽小字，作为确认信息 |
| 库存 | `sku.availableStock` | 购买前可读；提交时仍由 Java 再次校验 |
| 商品图 | `sku.imagePath` 或 `product.coverImagePath` | 优先当前 SKU 图片，失败时保留商品文字占位 |
| AI 入口 | `product.id`、`selectedSku.id` | 只传现有参数进入 AI 页面 |

## 5. 状态矩阵

| 状态 | 页面表现 | 禁止行为 |
| --- | --- | --- |
| Loading | 页面保留顶部上下文，显示简洁读取状态 | 不显示默认商品、默认价格或虚构 SKU |
| Error | 显示 `ApiError.message` 或安全错误文案，提供“重新加载” | 不因为 API 失败而静默切换 Mock |
| Not found / 无商品 | 显示无详情状态 | 不伪造商品内容 |
| Ready + ON_SALE | 商品图、价格、规格、库存和购买栏完整显示 | 不把真实接口包装成支付/发货承诺 |
| Ready + 非 ON_SALE | 真实商品信息可读，购买动作 disabled | 不显示可购买状态 |
| SKU 可购买 | 选项可点击，显示库存语义 | 不让前端库存替代提交时后端校验 |
| SKU 缺货 | 选项显示“缺货”、disabled、弱化视觉 | 不允许点击后继续购买 |
| Low stock | 显示“库存偏低”和真实数量 | 不显示虚构的“仅剩几件”营销文案 |
| Add loading | 加入购物车按钮显示“加入中…” | 不允许重复提交 |
| Buy loading | 立即购买显示“准备中…” | 不允许重复提交或提前跳转 |
| Add success | 通过 `MobileNotice` 显示真实成功反馈 | 不声称支付成功 |
| API action error | 显示安全错误信息和可重试语义 | 不吞掉后端错误 |
| Demo / Local Showcase | 延续项目已有边界标识 | 不把本地 fixture 伪装成生产订单或真实 Provider |

## 6. 移动端布局规格

### 390px 首屏目标

```text
顶部安全区 + 轻量导航：约 48px
商品舞台：约 300–340px，商品主体居中且不裁切
商品信息：名称、价格、状态紧随其后
规格选择：至少能看到标题和前两项
固定购买栏：不遮挡页面内容，遵守 safe-area
```

### 宽度规则

| 视口 | 规则 |
| --- | --- |
| 320–360px | 商品舞台缩小；规格选项保持可读；操作按钮不截断 |
| 390px | 主要验收尺寸；首屏应完整表达购买路径 |
| 768px 以上 | 允许内容更宽，但仍保持单一商品主线，不变成后台三栏 |

## 7. 视觉 Token

```css
.detail-page {
  --detail-canvas: #f5f5f7;
  --detail-surface: #ffffff;
  --detail-ink: #1d1d1f;
  --detail-muted: #6e6e73;
  --detail-line: #d2d2d7;
  --detail-blue: #0071e3;
  --detail-success: #248a3d;
  --detail-warning: #a05a00;
  --detail-danger: #c9343f;
}
```

- 页面底色和商品舞台有轻微表面差异，不使用渐变。
- 商品舞台可以使用较大的圆角；事实区和规格区不全部做成同样的圆角卡片。
- 主按钮使用蓝色，次按钮使用白底线框；同一操作栏不出现两个同权重的高饱和按钮。
- 系统字体优先：`-apple-system, BlinkMacSystemFont, "SF Pro Display", "PingFang SC", "Microsoft YaHei", sans-serif`。
- 技术 ID 使用等宽字体，但字号小、颜色弱，不承担主标题层级。

## 8. 组件和实现边界

### 保留

- `getProduct()`、`addCartItem()`、`ApiError`、`resolveImageUrl()`。
- `getStockPresentation()`、`isPurchasableSku()`。
- `ProductImage`、`MobileNotice`、`MobileStatusPill`、`MobileMoney`。
- `data-testid`（如后续测试使用）和现有路由参数。
- `addToCart()`、`buyNow()`、AI 跳转和返回逻辑。

### 本轮允许调整

- `detail.vue` 的模板层级、局部 class 和局部 style。
- 商品舞台、规格行、商品事实和购买栏的布局。
- 详情页需要的轻量本地状态，例如商品事实的展开/收起。

### 本轮不做

- 不改 Java、数据库、API、订单、库存和 AI 合同。
- 不接入真实 Provider，不新增支付、地址、物流和退款。
- 不修改首页、购物车、订单、AI 页面和 Admin。
- 不下载第三方 Logo、字体、图片或复制开源组件源码。

## 9. 截图验收表

| 检查项 | 通过标准 |
| --- | --- |
| 第一眼 | 明确是商品详情和购买页，不像后台或技术 Demo |
| 商品图 | 商品主体完整、清楚、无破图，成为第一视觉 |
| 信息层级 | 商品名、价格、库存、规格和 CTA 的主次明确 |
| SKU | 已选、可选、缺货、不可用不用猜 |
| 购买栏 | 不遮挡内容；加入购物车为次动作，立即购买为主动作 |
| 真实边界 | API、Demo、Mock、fallback 等语义不被夸大 |
| 中文排版 | 无重叠、无关键截断、英文技术字段不压过中文内容 |
| 响应式 | 390px 和 320px 无横向溢出；桌面预览不拉伸成奇怪的空白 |
| 无障碍 | 按钮可聚焦，SKU 使用 `aria-pressed`/disabled，focus-visible 可见 |
| 运行质量 | 控制台无新增 error；图片失败有语义化占位；loading 不可重复提交 |

## 10. 实施顺序

1. 先按本规格重排 `detail.vue`，只动这一页。
2. 运行 `npm test` 和 `npm run build`。
3. 启动真实本地 API 和 H5，打开 `productId=102`。
4. 生成 390×844 真实浏览器截图，并检查横向溢出、破图、控制台错误和按钮状态。
5. 如果视觉不通过，继续只返工商品详情页；通过后才进入首页。

## 11. 本轮实际落地结果

### 已修改

- `apps/mobile-app/src/pages/product/detail.vue`
- 页面模板从“技术标签 + 多个卡片”重排为“商品舞台 → 购买信息 → SKU 行式选择 → 商品事实 → AI 辅助 → 固定购买栏”。
- 真实 API、SKU 选择、库存 policy、加购、立即购买、AI 跳转和错误处理逻辑保留。

### 构建和测试

```text
npm test       31 passed
npm run build  passed
npm run build:uni  passed
git diff --check  passed
```

构建输出中的 Vite CJS、uni-app 更新提示和已有 uni-stat Vue3 warning 均不是本轮新增页面 error。

### 浏览器验收

- H5：`http://127.0.0.1:5173`
- Java API：`http://127.0.0.1:8083`
- 视口：`390 × 844`
- `productId=102`：单 SKU 商品图、价格、在售状态、商品事实入口和固定购买栏可读。
- `productId=101`：4 个真实 SKU 可读；白色 `S` 可切换；藏青色 `XL` 显示缺货并保持 disabled。
- 商品事实可以展开，能读到 Java 库存来源和下单再次校验说明。
- 真实点击“加入购物车”后显示“已加入服务端购物车”。
- CDP 布局测量：`scrollWidth=375`、`clientWidth=375`、`innerWidth=390`，未发现横向溢出。
- 控制台没有新增 error；保留项目原有的 uni-stat Vue3 warning。

当前预览：[商品详情页 productId=102](http://127.0.0.1:5173/#/pages/product/detail?productId=102)

本轮浏览器验收只证明页面结构、真实数据读取和交互链路；最终是否进入首页改造，仍以用户对这张详情页的视觉确认作为 Gate。
