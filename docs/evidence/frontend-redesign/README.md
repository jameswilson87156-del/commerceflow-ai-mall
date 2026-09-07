# 前端全量重做与浏览器验收记录

验收日期：2026-09-03  
项目：D:/workhome/commerceflow-ai-mall  
范围：apps/admin-web、apps/mobile-app  
结论：前端视觉重做第三轮（Apple-inspired system）已完成，本地真实链路已在浏览器跑通；后端、数据库和 API 字段契约未在本轮改变。

本文件中的 `screenshots/visual-qa-v3/` 是当前版本的验收证据。`screenshots/` 根目录下的旧截图仍保留，作为前两轮中间产物，不应再作为当前视觉效果的依据。

## 视觉与交互方向

- H5 采用 quiet gallery storefront：暖白画布、真实商品图优先、墨色信息、电光蓝购买动作、绿色库存/订单状态。
- Admin 采用 evidence-first operations cockpit：深海军导航、雾灰工作区、单层 context bar、主从选择和证据阅读顺序。
- 页面明确区分真实接口、LOCAL SHOWCASE、commerceflow-mock / MOCK、fallback、错误和编译边界；没有新增虚构指标或支付/物流状态。

完整的只读审查与逐页规格见 [FRONTEND_CURRENT_AUDIT](../../design/FRONTEND_CURRENT_AUDIT.md) 和 [FRONTEND_PAGE_SPECS](../../design/FRONTEND_PAGE_SPECS.md)。

## 真实浏览器截图（当前视觉 QA v3）

以下截图全部由本地 Vite 页面通过 Playwright CLI 浏览器真实打开并截图，不是设计稿或 AI 生成图。当前版本的设计取向是 Apple-inspired system：H5 使用浅灰系统画布、白色商品舞台、系统字体、真实商品主视觉和单一蓝色购买动作；Admin 使用同样的浅色系统表面，同时保留订单、库存、Evidence、Trace 的业务密度。仅借鉴留白、层级、控件克制和信息组织原则，没有复制第三方品牌页面。

| 页面 | URL | 视口 | 截图 |
| --- | --- | --- | --- |
| Admin 运营总览 | http://127.0.0.1:5174 | 1366×768 | [admin-overview-1366.png](screenshots/visual-qa-v3/admin-overview-1366.png) |
| Admin 商品与 SKU | http://127.0.0.1:5174 → 商品与 SKU | 1366×768 | [admin-product-sku-1366.png](screenshots/visual-qa-v3/admin-product-sku-1366.png) |
| Admin 订单执行证据 | http://127.0.0.1:5174 → 订单管理 | 1366×768 | [admin-order-evidence-1366.png](screenshots/visual-qa-v3/admin-order-evidence-1366.png) |
| Admin AI 工作台 | http://127.0.0.1:5174 → AI 客服 | 1366×768 | [admin-ai-workbench-1366.png](screenshots/visual-qa-v3/admin-ai-workbench-1366.png) |
| H5 商品列表 | http://127.0.0.1:5173/#/pages/index/index | 390×844 | [mobile-product-list-390.png](screenshots/visual-qa-v3/mobile-product-list-390.png) |
| H5 商品详情 | http://127.0.0.1:5173/#/pages/product/detail?productId=101 | 390×844 | [mobile-product-detail-390.png](screenshots/visual-qa-v3/mobile-product-detail-390.png) |
| H5 购物车 | http://127.0.0.1:5173/#/pages/cart/index | 390×844 | [mobile-cart-390.png](screenshots/visual-qa-v3/mobile-cart-390.png) |
| H5 订单确认 | http://127.0.0.1:5173/#/pages/order/confirm | 390×844 | [mobile-order-confirm-390.png](screenshots/visual-qa-v3/mobile-order-confirm-390.png) |
| H5 订单结果 | http://127.0.0.1:5173/#/pages/order/result?orderNo=... | 390×844 | [mobile-order-result-390.png](screenshots/visual-qa-v3/mobile-order-result-390.png) |
| H5 订单详情 | http://127.0.0.1:5173/#/pages/order/detail?orderNo=... | 390×844 | [mobile-order-detail-390.png](screenshots/visual-qa-v3/mobile-order-detail-390.png) |
| H5 订单列表 | http://127.0.0.1:5173/#/pages/order/list | 390×844 | [mobile-order-list-390.png](screenshots/visual-qa-v3/mobile-order-list-390.png) |
| H5 AI 商品客服 | http://127.0.0.1:5173/#/pages/ai/customer-service?productId=101&skuId=10004 | 390×844 | [mobile-ai-390.png](screenshots/visual-qa-v3/mobile-ai-390.png) |

截图中的数据是当前本地 Showcase 数据，不代表真实客户、商业收入或生产库存。订单结果只表示服务端 CREATED，不表示支付、发货或物流完成。

## 浏览器验收结果

### Admin

使用干净浏览器页签在 1366×768 复核四个工作区：

- 运营总览：真实商品、库存、订单、AI Trace 和运行边界可读。
- 商品与 SKU：Product → SKU → Inventory 主从选择可读，图片路径正常，库存正常/偏低/缺货状态可区分。
- 订单执行证据：左侧真实订单列表与右侧订单摘要、OrderItem 图片快照、stockBefore / quantity / stockAfter、ORDER_DEDUCT、幂等键和事务说明可读。
- AI 工作台：选择真实 SKU 后完成一次问答，实际显示 commerceflow-mock / MOCK、Redis 限流额度、Evidence 和 6 步 Trace。
- 四页均满足 document.documentElement.scrollWidth <= window.innerWidth，破图数量为 0；干净页签控制台没有 error/warn。

#### Admin AI 工作台响应式收口（EC-UI-01）

2026-09-03 追加复核了视觉覆盖层之后的实际断点，确认手机端不会重新继承桌面固定列宽：

| 视口 | 预期布局 | 实际结果 |
| --- | --- | --- |
| 390×844 | 单列，商品/SKU → 对话 → 事实证据纵向阅读 | 单列，三个面板宽度均为 343px，无横向溢出 |
| 768×844 | 单列，保留完整事实面板 | 单列，三个面板宽度均为 721px，无横向溢出 |
| 1024×900 | 左侧选择 + 中间对话，事实面板换行到下一行 | `280px + 462.667px`，事实面板占整行，无横向溢出 |
| 1440×900 | 商品/SKU + 对话 + 事实证据三列 | `290.052px + 475.375px + 362.562px`，无横向溢出 |

四个视口的破图数量均为 0，控制台没有 error/warning。AI 筛选控件也通过显式 `id`/`label for` 关联，加载、错误、切换提示和输入错误补充了对应的 live region。

#### H5 商品发现层（EC-UI-02）

商品列表现在在真实 `GET /api/products` 已载入数据上提供可验证的本地搜索、分类和排序：

| 验收项 | 实际结果 |
| --- | --- |
| 搜索 | 输入“托特”后从 2 件真实商品筛出 1 件；搜索结果数量通过 live region 更新 |
| 分类 | 分类按钮由 API 的 `categoryName` 去重生成；选择“配饰 / 包袋”后只保留对应商品 |
| 排序 | 排序按钮在推荐、价格升序、价格降序、商品名称之间循环，不改变原始响应数组 |
| 无匹配 | 显示“没有找到与…相关的商品”和“清除筛选”，不插入 fixture |
| 响应式 | 390×844：`scrollWidth=375`、2 张商品卡、破图 0；768×900：`scrollWidth=753`、2 张商品卡、破图 0 |

实现文件：`apps/mobile-app/src/ui/mobile-catalog-policy.mjs`、`apps/mobile-app/src/pages/index/index.vue`；策略层由移动端单测覆盖。

## 最终验收追加记录：2026-09-03 PHASE_9G

本节记录本轮前端收口后的最终验收，不覆盖前面的阶段证据。验证使用本地 Showcase 服务和合成数据，未调用外部模型、未读取真实密钥、未修改 Java/FastAPI/SQL 契约。

### 自动化结果

| 范围 | 命令 | 实际结果 |
| --- | --- | --- |
| Admin 单元 / 组件 | `apps/admin-web/npm test` | 6 个测试文件、45/45 通过 |
| Admin 生产构建 | `apps/admin-web/npm run build` | `vue-tsc` 与 Vite 构建通过 |
| H5 策略与运行时辅助 | `apps/mobile-app/npm test` | 35/35 通过，包含 EC-UI-02 搜索、分类、排序测试 |
| H5 生产构建 | `apps/mobile-app/npm run build` | 通过；仅保留 UniApp 已有提示 |
| UniApp 编译 | `apps/mobile-app/npm run build:uni` | 通过；这是编译证据，不是原生设备运行证据 |
| Java API 回归 | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 53/53 通过，0 failure/error/skip |
| FastAPI 回归 | `py -3 -m pytest`（`services/ai-service`） | 11/11 通过 |
| Showcase 运行验收 | `scripts/showcase/verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke` | `SHOWCASE_VERIFY_OK` |

### 真实浏览器结果

- H5 在干净临时页签验证了 `390×844`、`768×1024`、`1024×900`、`1440×1000`；初始目录均显示 2 张真实商品卡，`scrollWidth` 没有超过可视宽度，破图数量均为 0。
- H5 搜索“托特”得到 1 件，选择“配饰 / 包袋”得到 1 件，排序按钮切换到“价格由低到高”；结果数量、无匹配边界和分类入口均来自已加载的真实 API 商品响应，不会插入 Mock fixture。
- Admin 在 `390×844`、`768×1024`、`1024×900`、`1440×1000` 均无横向溢出，`main` 与页面级 `h1` 存在，破图数量为 0。
- Admin AI 客服实际走 Java API：选择真实灰色 L SKU，发送库存问题，页面返回 `commerceflow-mock / MOCK`、Redis 配额、7 条 Java Evidence 和 6 步 Trace；页面明确显示未调用外部大模型。
- Showcase 脚本同时验证了真实订单创建、同一 `Idempotency-Key` 重放、不同请求体返回 `409`、订单执行证据，以及 5 次成功后第 6 次 `429` 和限流响应头。

### 可访问性与遗留边界

- Admin 的本轮前端基础审查已覆盖显式 label、focus-visible、响应式、键盘可用性和页面 live 状态；电商仓库没有引入独立 axe/Lighthouse runner。
- `npm run build` 会保留 Vite CJS API、UniApp 版本提示；H5 运行时仍有项目原有 `uni-stat 2.0` 的 Vue3 mixin warning，未观察到页面 JavaScript exception。
- 当前测试证明的是本地功能和边界，不证明支付、物流、退款、生产认证、吞吐、商业收入或原生 Android/iOS/小程序运行时。

### H5

使用干净浏览器页签在 390×844 复核商品列表、详情、购物车、订单确认、订单结果、订单详情、订单列表和 AI 页面：

- 所有页面都满足 scrollWidth <= innerWidth，破图数量为 0。
- 商品详情默认选择可购买 SKU；选中状态使用边框、文案和 aria-pressed，缺货 SKU 明确显示“缺货”，固定购买栏包含购物车、加入购物车和立即购买。
- “立即购买”调用现有真实加购 API 后进入订单确认；订单确认重新读取服务端购物车，创建真实 CREATED 订单，结果页和详情页读取服务端返回的订单与历史快照。
- AI 页面完成一次真实请求，显示 commerceflow-mock / MOCK、Redis 限流正常、回答和可折叠的 Business Facts / Evidence / Trace。
- 控制台没有 error；存在 1 条项目原有的 uni-stat 2.0 Vue3 warning：onCreateVueApp ... 页面级 mixin 未注入。该 warning 来自 @dcloudio/uni-stat，不是本轮页面代码报错，已在限制项中保留说明。

## 自动化与本地 Showcase 命令

本轮前端修改完成后已执行：

    Set-Location apps/admin-web
    npm test                 # 45 tests passed
    npm run build            # exit 0

    # EC-UI-01 追加验收
    # 390 / 768 / 1024 / 1440 四个视口：布局、溢出、破图、控制台检查通过

    Set-Location ../mobile-app
    npm test                 # 35 tests passed（包含 EC-UI-02 目录策略）
    npm run build            # exit 0
    npm run build:uni        # exit 0，保留 uni-app 既有提示

项目完整基线在本次前端工作前已通过：Java 53 tests, 0 failure/error/skip、Python 11 passed、仓库完整性检查 REPOSITORY_INTEGRITY_OK。前端本轮没有修改 Java、FastAPI、SQL migration、后端测试或 API helper。

本轮视觉 QA 使用 Java 8083、Python 8000、Admin 5174、H5 5173。Admin/H5 通过 Vite 代理访问本地 Java API；Java 8083 是为避开本机 8080 占用而使用的验收端口，不改变项目默认前端端口。

此前的本地 Showcase 基线使用 Java 8081（因 8080 有外部占用）、Python 8000、Admin 5174、H5 5173，并通过：

    powershell -NoProfile -File .\scripts\showcase\verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke

验证真实商品、购物车、AI Provider/Evidence/Trace、订单首次创建/同 Key 重放/冲突 409 和 5 次成功 + 1 次 429 限流边界。Provider 当前是本地确定性 commerceflow-mock，没有真实 API Key，也没有公共部署、域名、DNS 或云环境。

## 修改与边界

新增通用组件：

- Admin：AdminStatusPill.vue、AdminSectionHeader.vue、AdminBoundaryStrip.vue
- H5：MobileBottomNav.vue、MobileStatusPill.vue、MobileMoney.vue

重构内容：

- Admin App.vue 与 style.css：深色导航、context bar、响应式断点、focus-visible、reduced-motion 和新的事实面板层级。
- Admin 四个工作区：运营信号、Product/SKU 主从层级、订单证据阅读顺序、AI 三栏工作区。
- H5 MobileHeader.vue、主题 token 以及列表、商品详情、购物车、订单确认/结果/详情/列表、AI 页面模板和样式。

没有删除已有业务能力或 API；旧的 ProductImage.vue、MobileNotice.vue、API helper、policy helper 和测试选择器继续保留。没有部署、提交、推送、改 DNS、添加密钥或修改另一个项目。

已知限制：

- userId=1 是演示身份，订单状态范围只有 CREATED。
- Provider 是 Mock；FastAPI/Provider 不可用时的 Java fallback 仅是 Showcase 安全降级。
- 非 H5 UniApp 目标仍是 compile-only，未宣称原生设备验收。
- uni-stat 2.0 的既有 Vue3 warning 尚未由本项目控制。
- 运行过程产生的订单和 AI Trace 会写入本地合成数据。

最小下一步：先让你直接查看 `screenshots/visual-qa-v3/` 的当前效果；如果这套 Apple-inspired 视觉方向确认，再补 CI 截图快照任务、真实 Provider 接入和公网部署，不要把三件事混在一次改动里。
