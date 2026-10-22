# CommerceFlow 两端统一视觉规范

## 1. 设计方向

管理端呈现清楚、可信、便于扫描的运营事实；用户端呈现适合移动购物的商品与购买流程。两端统一语义颜色、字体可读性和交互反馈原则，不要求布局同构或将每个区域做成卡片。

## 2. CSS 导入图与令牌所有权

### 管理端导入图

apps/admin-web/src/main.ts
→ App.vue（静态导入四个页面视图和组件）
→ 入口显式导入 style.css
→ 入口显式导入 craft.css

main.ts 静态依赖先加载 App.vue，随后按源码顺序显式导入 style.css、craft.css；全局 CSS 的明确顺序是 style.css → craft.css。style.css 的三个 :root 块按文件顺序依次覆盖；craft.css 的根规则和后续选择器在其后参与级联。页面 SFC 没有 scoped 样式；AdminAuthControl.vue 的 scoped 样式只作用于该组件。不能把源码令牌值当成元素最终值：例如 craft.css 的 --canvas 是 #f7f8f9，但 style.css 后段对 body、.app-shell 和页面仍有 #f5f5f7 硬编码；--blue 已被 craft.css 改为 #245b49，后段按钮/选中规则和 AdminAuthControl 仍写 #0071e3。

### 用户端导入图

apps/mobile-app/src/main.ts
→ App.vue（静态导入 pages.json 映射的页面；App.vue 样式导入 styles/theme.css）
→ platform/h5.ts
→ styles/craft.css
各页面 SFC 的非 scoped 样式随页面模块导入

craft.css 在入口中后于 App.vue 导入，并通过 html:root 覆盖 theme.css 的部分 cf 变量。各页面 SFC 的 style 块没有统一 scoped，可能与 theme.css/craft.css 的同名规则同时作用；改动前需检查选择器权重、实际加载顺序和媒体查询。

### 当前根令牌审计与归属

| 语义 | 管理端当前有效来源和值 | 用户端当前有效来源和值 | 归属与约束 |
| --- | --- | --- | --- |
| 页面背景 | craft.css 令牌 --canvas #f7f8f9；body/.app-shell 有后段硬编码 #f5f5f7 | craft.css：--cf-page #faf9f6 | 各端独立；管理端需迁移直接背景值以消除令牌与页面实色分歧 |
| 内容面板 | craft.css：--panel #fff | theme.css：--cf-surface #fff | 角色可共享，名称和布局归所属应用 |
| 主文字 | craft.css：--ink #24312d | craft.css：--cf-ink #292d24 | 语义共享，当前值不同 |
| 次级文字 | craft.css：--muted #64716d | craft.css：--cf-muted #686d62 | 语义共享，当前值不同 |
| 边界 | craft.css：--line #e0e6e3 | craft.css：--cf-line #dcded5 | 语义共享，当前值不同 |
| 品牌/交互 | craft.css：--blue #245b49 | craft.css：--cf-blue #354c35 | 当前都是绿调但值不同；不声称已统一 |
| 成功 | style.css 保留 --cyan #248a3d | theme.css 保留 --cf-green #248a3d | 语义和值目前一致 |
| 警告 | style.css 保留 --amber #a05a00 | theme.css 保留 --cf-orange #a05a00 | 语义和值目前一致 |
| 错误 | style.css 保留 --red #c9343f | theme.css 保留 --cf-red #c9343f | 语义和值目前一致 |
| 信息/选中 | 多处直接写色值；部分引用 --blue | 多处使用 --cf-blue 或直接写色值 | 逐个核对，不新增第三套品牌色 |
| 字体 | craft.css：Segoe UI Variable、PingFang SC、Microsoft YaHei | theme.css 使用系统字体、PingFang SC、Microsoft YaHei | 中文优先的字体角色可共享；平台回退顺序可独立 |
| 页面间距 | style.css 页面级固定值与断点；无统一根令牌 | theme.css 定义 --cf-page-padding、底栏高度/间隙 | 页面结构与底栏尺寸必须端侧独立 |
| 圆角/阴影 | 根阴影在 craft.css 关闭；组件有局部值 | theme.css 有阴影；craft.css 覆盖部分阴影和圆角 | 控件尺度可对齐，不强制统一卡片外观 |

管理端 style.css 前段、Frontend redesign layer 和 Apple-inspired layer 有三套根定义；最后导入的 craft.css 再覆盖画布、面板、文字、边线、主色、浅主色、阴影与字体。未在 craft.css 重定义的导航、成功、警告、错误变量仍取 style.css 最后一组根令牌。页面和组件中仍存在硬编码颜色、字体大小、间距、圆角与阴影。

用户端 theme.css 是基础令牌与购物公共样式；craft.css 的 html:root 通过更高选择器优先级覆盖页面背景、次级背景、边线、文字、主色、浅主色及阴影，但成功、警告、错误色和底栏尺寸继续来自 theme.css。首页、商品详情、购物车、订单和客服页面的全局 SFC 样式还有多套重复颜色和尺寸覆盖。

## 3. 最小共享设计令牌

跨端共享以下语义角色与验收要求。两个应用各自在自己的根作用域定义同名语义变量，映射到本端颜色和布局，禁止跨应用导入样式文件：

- 品牌主色与交互强调色。
- 主文字、次级文字、边界。
- 成功、警告、错误、信息状态。
- 中文基础字体与标题/正文/辅助文字层级。
- 4px 基础步进的间距参考、4/6/8/12px 圆角参考。
- 焦点、禁用、悬停、选中和减少动态效果状态。

最小语义变量名：`--brand-primary`、`--brand-soft`、`--text-primary`、`--text-secondary`、`--surface-page`、`--surface-panel`、`--border-subtle`、`--state-success`、`--state-warning`、`--state-danger`、`--state-info`、`--focus-ring`、`--font-body`、`--space-unit`、`--radius-control`、`--radius-panel`。各端可映射不同值；导航/表格密度、购物底栏/安全区、商品画幅、断点和局部例外归各自应用。

这是共享的语义合同，不要求当前 CSS 变量同名或数值立即相同。管理端导航、表格密度和筛选布局，以及用户端安全区、购买底栏和商品图片区，必须保留端侧令牌。不得将状态色只用于装饰，也不得用品牌色替代语义色。

## 4. 组件复用与样式迁移纪律

管理端优先复用 AdminSectionHeader、AdminStatusPill、AdminBoundaryStrip、AdminAuthControl；用户端优先复用 MobileHeader、MobileBottomNav、MobileStatusPill、MobileNotice、MobileMoney、ProductImage。两端为独立应用，禁止为了表面统一跨端复制组件。

每次 CSS 修改遵循以下步骤：

1. 记录工作区状态、分支、HEAD 和目标文件哈希；先确认规则来源与导入顺序。
2. 一阶段只迁移一个页面或一组明确相关选择器；优先复用语义令牌，新增变量先确认所属应用。
3. 不用宽泛选择器、!important 或全局覆盖层规避权重问题；先移除或修改明确的重复声明。
4. 保留现有断点、键盘焦点、禁用、选中和 reduced-motion 行为；在目标视口实际检查。
5. 运行该阶段已有测试与必要构建；失败就停在当前阶段，定位回退本阶段差异。
6. 不全仓格式化，不覆盖旧截图或构建产物；新截图放到独立路径并记录。

V3 允许在完成审计后逐组迁移上述语义变量和使用点。一次只调整一个共享令牌组或一个页面；先哈希记录目标文件，补充/修改变量后检查所有引用与同名覆盖，再运行对应测试并在 1440、1280、768、390、360px 检查。不得用全局通配、`!important` 或整文件替换规避级联；不合并两端 CSS，不新增不必要依赖，不改变表格或购物流程。

## 5. 两端布局与可访问性

管理端保留工作区、数据摘要、筛选、表格和业务工作流。桌面优先，窄屏表格可以局部滚动；页面不能整体水平溢出。用户端保留商品图片、详情、购物车、订单、客服和底部导航；移动屏幕优先。必须覆盖桌面 1440/1280px、窄屏 768px 和手机 390/360px；若实际媒体断点更严格，再补测该断点。

可聚焦控件必须有明显焦点轮廓；禁用和选中态需可辨；状态同时有文字；重要操作不能只用图标表达；减少动态效果偏好继续有效。

## 6. 参考证据

本次核验时 Foundry 提交为 5d6de55739de135bdf73cae05c342fa3bea3edd4，PE Studio 提交为 5b2791ae46626a81e54d66c40415a8ddc4dcc8bd。

A级源码证据：
- Foundry 的 AionUi/packages/desktop/src/renderer/components/chat/CommandQueuePanel.tsx 展示了队列项操作、异步新增反馈、交互锁和桌面/窄屏不同拖动方式。该文件未证明有任务优先级排序。
- PE Studio 的 app/globals.css、components/studio/provider-settings.css、components/studio/settings-dialog.tsx 展示了状态颜色/焦点/响应式样式、设置草稿状态与保存过程。

B级文档证据：Foundry README 与 PE Studio README 描述各自产品方向与功能；未在源码中验证的部分仅视作文档所述。

C级结论：未读源码、未执行构建或未在目标系统运行的项目行为仍属未验证，不得写为已实现或已验收。

## 7. 阶段与回退

按 V3 批次执行：工作区和 12 页清单 → CSS/令牌审计 → 四份规范自审 → 运营总览 → 共享令牌/公共组件 → 4 个管理端视图 → 8 个用户端页面 → 双端测试/类型检查/隔离构建/浏览器回归 → 独立差异审查。每页逐项记录实现、测试和浏览器状态；一个页面的视觉通过不能替代其他页面验收。

无浏览器或服务条件时，对应页面只能标 `BLOCKED` 或 `TESTED`，不能标 `BROWSER_VERIFIED`；继续处理不依赖阻塞项的页面。测试或构建不能代替视觉验收。失败时保存完整错误，先判断基线/本轮来源，最多进行三轮有根因的修复；只回退本轮可准确识别的文件差异，保留用户已有修改与旧截图/构建产物。
