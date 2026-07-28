# P6 UniApp 官方研究

## 研究记录

- 研究日期：2026-07-29
- 研究工具：本机未安装 `agent-reach` CLI（`agent-reach doctor --json` 不可执行）；使用联网只读官方 DCloud 文档、官方 GitHub 仓库和 Vue 官方文档完成 fallback 研究。
- 未下载、克隆或复制任何外部仓库代码。

| 官方来源 | 关键结论 | CommerceFlow 采用 |
| --- | --- | --- |
| [uni-app 页面](https://uniapp.dcloud.net.cn/tutorial/page.html) | 页面是 Vue SFC；页面必须在 `pages.json` 的 `pages` 中注册；除 Vue 生命周期外还有 `onLoad`、`onShow` 等页面生命周期。 | P6B 将页面拆分为真实已注册页面，并在返回/再显示时刷新服务端状态。 |
| [pages.json](https://uniapp.dcloud.net.cn/collocation/pages) | 决定路由、窗口、原生导航栏和 tabBar；tabBar 高度有平台约束。 | 采用真实 tab/路由；不把未实现的 Orders 文本留在 tabBar。 |
| [uni.request](https://uniapp.dcloud.net.cn/api/request/request.html) | 支持 GET/POST、状态码、响应 headers、超时和 `RequestTask.abort()`；H5 有 CORS；手机不能访问电脑的 localhost。 | 统一 request wrapper 读取 headers、429、错误和取消；地址通过环境注入。 |
| [UniApp Vue3](https://uniapp.dcloud.net.cn/tutorial/vue3-basics.html) | Vue3 在 H5、小程序、App 的支持边界不同；Vue3 编译链使用 Vite。 | 保持 Vue3/Vite；跨端只用可验证的通用语法和条件编译。 |
| [条件编译](https://uniapp.dcloud.net.cn/tutorial/platform.html) | `#ifdef/#ifndef` 可针对 H5、App、小程序编译；条件编译需保证各平台语法都成立。 | 仅用于地址、平台能力和必要的安全区差异，不复制三套业务逻辑。 |
| [页面样式与安全区](https://uniapp.dcloud.net.cn/tutorial/syntax-css.html) | 提供 `--status-bar-height`、`--window-bottom` 等变量；不同平台导航栏/tabBar有差异。 | 底部操作区使用安全区间距，验收时检查 390x844/430x932。 |
| [manifest.json](https://uniapp.dcloud.net.cn/collocation/manifest) | 应用名、appid、版本、networkTimeout 和 H5/App/小程序平台配置位于 manifest。 | P6B 只在需要时配置超时和平台地址，不把真实环境值提交到 manifest。 |
| [uni-app 官方仓库](https://github.com/dcloudio/uni-app) | 官方跨端框架仓库，公开页面标示 Apache-2.0；支持 H5、App、小程序等多端。 | 仅使用框架能力和官方文档；不复制示例业务代码。 |
| [Vue 3 官方介绍](https://vuejs.org/guide/introduction.html) | Vue 是组件化、声明式 UI 框架，Vue3 文档对应当前主线。 | 使用 Composition API 组织页面和最小 composable。 |

## 适用性判断

这些资料适用于 P6 的路由注册、生命周期、请求封装、安全区、条件编译和构建策略；不证明 CommerceFlow 的 Java API、真机兼容性或登录/支付能力已经存在。`npm run build` 或 `npm run build:uni` 只能证明编译链通过，不能证明 H5、Android、iOS 或小程序真实运行通过。

