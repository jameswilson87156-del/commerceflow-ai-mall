# P6 移动端研究来源记录

| 来源 | URL | 研究日期 | 研究内容 | CommerceFlow 处理 |
| --- | --- | --- | --- | --- |
| uni-app 官方仓库 | https://github.com/dcloudio/uni-app | 2026-07-29 | 跨端能力、目录、许可证展示 | 只使用框架能力，不复制业务代码。 |
| uni-app 官方页面 | https://uniapp.dcloud.net.cn/tutorial/page.html | 2026-07-29 | pages.json 注册、页面生命周期 | 作为 P6 路由与刷新设计依据。 |
| uni-app pages.json | https://uniapp.dcloud.net.cn/collocation/pages | 2026-07-29 | 页面、导航、tabBar、安全区相关配置 | 作为导航设计依据。 |
| uni.request | https://uniapp.dcloud.net.cn/api/request/request.html | 2026-07-29 | 请求、header、状态码、超时、abort、CORS | 作为 request wrapper 设计依据。 |
| uni-app 跨端/条件编译 | https://uniapp.dcloud.net.cn/tutorial/platform.html | 2026-07-29 | H5/App/小程序条件编译 | 仅用于平台差异，不分裂业务逻辑。 |
| uni-app CSS/安全区 | https://uniapp.dcloud.net.cn/tutorial/syntax-css.html | 2026-07-29 | `--status-bar-height`、`--window-bottom` | 作为移动底部操作区验收依据。 |
| CRMEB Java | https://github.com/crmeb/crmeb_java | 2026-07-29 | 商品、SKU、购物车、订单、多端组织 | 只做流程/结构研究，无代码复用。 |
| macrozheng/mall | https://github.com/macrozheng/mall | 2026-07-29 | 前台商城领域范围与移动端技术选型 | 只做领域对照，不引入其基础设施。 |
| macrozheng/mall-app-web | https://github.com/macrozheng/mall-app-web | 2026-07-29 | UniApp Vue3 的移动页面和目录组织 | 只参考目录边界和状态组织。 |
| linlinjava/litemall | https://github.com/linlinjava/litemall | 2026-07-29 | 商品、购物车、订单和小程序/移动端范围 | 只参考流程；认证、上传、安全、支付排除。 |

## 无外部资产声明

本轮没有把上述项目整仓放入 CommerceFlow，也没有复制其 Java、Vue、UniApp、SQL、图片、Logo、字体、登录、支付、上传或安全实现。P6 规划只引用公开 URL、许可证和抽象设计观察。
