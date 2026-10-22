# CommerceFlow 前端中文化与统一视觉改造设计稿

## 1. 目标与当前阶段

管理端面向中国商家与运营人员，优先呈现经营事实、检索条件和处理入口；用户端继续服务商品浏览、购物、订单与客服流程。两端共享清晰一致的品牌语义，不要求布局、密度或组件结构相同。

V3 授权范围覆盖管理端全部 4 个内部视图和用户端 `pages.json` 注册的全部 8 个 H5 页面。实施按“工作区与页面基线 → CSS/令牌审计 → 规范自审 → 共享样式与组件 → 管理端逐页 → 用户端逐页 → 双端回归与浏览器验收 → 差异复核”推进。每页状态使用 `NOT_STARTED`、`IN_PROGRESS`、`IMPLEMENTED`、`TESTED`、`BROWSER_VERIFIED` 或 `BLOCKED`；完成文件编辑不能代替测试或浏览器验收。允许逐批迁移展示文案、页面样式、全局令牌和现有 UI 组件；后端、API 契约、权限、订单/库存/支付逻辑和数据来源仍在禁止修改范围内。

## 2. 实际代码基线与 CSS 导入图

以下结论来自 2026-10-21 对目标工作区真实文件的只读检查。

### 管理端

入口是 apps/admin-web/src/main.ts：静态依赖先导入 App.vue（App 再静态导入四个页面和管理端组件），入口随后显式导入 style.css 和 craft.css。显式全局覆盖顺序为 style.css → craft.css；各 Vue SFC 的样式由其页面模块导入。App.vue 选择运营总览、商品与 SKU、订单管理、AI 客服，并复用 AdminAuthControl。页面组件引用关系如下：

- OperationsOverview.vue 使用 AdminSectionHeader、AdminStatusPill、AdminBoundaryStrip。
- ProductSkuManagement.vue 与 OrderInventoryEvidence.vue 复用上述页头、状态标签和边界组件。
- AdminAuthControl.vue 使用带 scoped 的组件样式；管理端页面主要依赖全局 style.css 与 craft.css。

### 用户端

apps/mobile-app/src/main.ts 导入 App.vue、H5 平台适配和 styles/craft.css；App.vue 通过样式导入 styles/theme.css，并显式注册 pages.json 中的购物页面。H5 页面由 App.vue 的路径表切换。页面 SFC 的 style 块未统一使用 scoped，必须把它们视为可能影响全局同名选择器的样式来源。完整令牌与覆盖关系见 FRONTEND_VISUAL_SYSTEM.md。

实际级联要点：

- 管理端 style.css 内有三次根令牌定义，后定义覆盖先定义；其后的 craft.css 再覆盖部分根令牌。style.css 后段仍有直接颜色值，例如 body/app-shell 的 #f5f5f7 背景和按钮/导航的 #0071e3 强调色，因此根变量最终值不代表所有元素的最终颜色。
- 用户端 theme.css 定义 cf 令牌，craft.css 的 html:root 以更高选择器优先级改写其中一部分；其他状态色与移动专用尺寸仍由 theme.css 提供。
- 多个 SFC 样式包含直接颜色、尺寸、圆角和断点。修改前要沿入口追踪顺序，并确认目标规则的选择器权重及媒体查询条件。

## 3. 最小共享视觉语义

两端以语义角色共享设计约定：品牌/交互强调、主文字、次级文字、边界、成功、警告、错误、信息、基础字体、字号层级、间距步进、圆角步进、焦点、禁用、悬停与选中状态。

这代表共享含义与验收方式，不代表当前 CSS 必须使用相同颜色值。当前管理端令牌主色为 #245b49、用户端为 #354c35；两端文字、背景和边线色也不完全相同。V3 允许在完成审计后逐组建立语义别名并迁移明确相关的规则；不得用一次全局替换压掉端侧差异，也不得宣称两端数值已经统一。

圆角可从 4/6/8/12px 等级按控件与容器选用；间距按 4px 基础步进规划。实际页面可以使用端侧布局令牌与局部例外，不得为了统一而将所有内容包进卡片。

## 4. 布局边界

### 管理端

保留侧边导航、数据摘要、筛选、表格、订单/库存工作流与真实状态。桌面优先，窄屏允许表格内部横向滚动，但页面主体不可整体溢出。运营总览保持高密度、明确的数据层级与真实边界说明。

### 用户端

保留移动商品图、商品详情、购物车、订单、客服、底部导航与购买操作。移动优先；宽屏样式属于当前页面实现的一部分，需与窄屏一起验收。不得以管理端卡片或表格结构替代购物流程。

## 5. 现有公共组件复用

优先在各自应用内复用现有组件：

- 管理端：AdminSectionHeader、AdminStatusPill、AdminBoundaryStrip、AdminAuthControl。
- 用户端：MobileHeader、MobileBottomNav、MobileStatusPill、MobileNotice、MobileMoney、ProductImage。

两端是独立应用，组件运行环境和交互语义不同；默认不跨应用直接导入组件。只有现有组件无法满足且有具体证据时，才提出新增组件，并先说明复用缺口。不得复制一份标题、状态标签或权限控件来做相同用途。

## 6. 文案、数据与业务安全

普通用户文案使用自然简体中文。SKU、AI、API 等必要术语可以保留并配合中文语境。接口路径、响应字段、枚举、日志标识和错误码保持原样；仅在展示层映射为中文。高级诊断区可展示确有排查价值的原始信息。

任何数字、状态、数据范围、运行模式和成功提示必须来自现有接口或真实页面状态。空数据、加载失败、未登录、无权限、只读和成功不得混淆。不得更改请求路径、参数、权限策略、订单/库存行为或后端逻辑。

## 7. 参考项目及证据等级

参考仓库在核验时的提交为 Foundry 5d6de55739de135bdf73cae05c342fa3bea3edd4，PE Studio 5b2791ae46626a81e54d66c40415a8ddc4dcc8bd。

- A｜已读取源码：Foundry 的 AionUi/packages/desktop/src/renderer/components/chat/CommandQueuePanel.tsx 可支持队列状态、异步反馈、操作和窄屏交互方面的参考；本次检查没有在该组件中确认任务优先级排序。PE Studio 的 app/globals.css、components/studio/provider-settings.css 与 components/studio/settings-dialog.tsx 可支持令牌引用、焦点/禁用/响应式样式和设置草稿状态管理的具体参考。
- B｜已读取项目文档：Foundry README 与 PE Studio README 描述了产品工作台、设置和异步交互等产品意图；未由 README 单独推断为已验证实现。
- C｜尚未验证：未检查的源码、运行效果、平台行为和性能结论不得写成确定事实。

只借鉴可迁移的任务状态提示、异步反馈、工作区组织、焦点状态、响应式布局和设置状态管理思路。不得复制 PE Studio 的整套 CSS；Foundry 的 Electron/Rust 架构、原生玻璃材质、浮动窗口及桌面专属交互不属于迁移范围。不得新增组件库、动画引擎或平台专用依赖。

## 8. 实施与回退纪律

按 V3 批次执行：读取真实工作区和四份规范；建立 CSS/令牌与 12 个页面清单；修订并自审四份现有规范；复核运营总览；逐组迁移共享语义令牌/组件；逐页改造 4 个管理端视图与 8 个用户端页面；运行所属应用测试、安全构建和浏览器验收；最后复核所有差异、产物和原有工作区保护情况。每阶段先记录当前分支、HEAD、工作区状态和涉及文件哈希。不得 reset、clean、强制 checkout、全仓格式化或覆盖用户既有文件；不提交、推送、合并或部署。

出现测试失败、API/权限/业务行为变化或布局退化时，暂停受影响页面及依赖它的组件；只撤销本轮明确产生且可安全识别的差异，保留原有修改，并记录失败证据、回退范围和重新验收条件。无浏览器条件时，对应页面标为 `BLOCKED` 或 `TESTED`，不能标为 `BROWSER_VERIFIED`；继续处理不依赖该阻塞项的页面。
