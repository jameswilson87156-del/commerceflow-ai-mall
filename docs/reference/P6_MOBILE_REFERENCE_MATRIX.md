# P6 移动端参考矩阵

| 来源 | 可研究的页面/模式 | 许可证与使用边界 | CommerceFlow 可采纳 |
| --- | --- | --- | --- |
| [crmeb/crmeb_java](https://github.com/crmeb/crmeb_java) | 商品、SKU、购物车、订单、多端 UniApp 组织方式、API 分层 | 仓库公开标示 Apache-2.0；仍需逐文件核对声明 | 只研究页面分区、请求分层和业务顺序；不复制整页、数据库、认证、支付或素材。 |
| [macrozheng/mall](https://github.com/macrozheng/mall) | 商品展示、SKU、购物车、订单流程；后端模块拆分 | Apache-2.0；工程包含 MyBatis、Redis 等大量非 P6 技术 | 研究领域流程和字段组织；不引入其基础设施，不复制实现。 |
| [macrozheng/mall-app-web](https://github.com/macrozheng/mall-app-web) | UniApp+Vue3 移动端目录、商品搜索/展示、购物车、订单；`apis`、`pages`、`stores`、`types`、`utils` 分层 | Apache-2.0 | 参考目录边界、Pinia 何时有价值、空/加载/错误页面；不复制组件、图片或整页。 |
| [linlinjava/litemall](https://github.com/linlinjava/litemall) | 商品、购物车、下单、订单列表/详情、微信小程序与 Vue 移动端 | GitHub 页面标示 MIT；文档另有 CC BY-ND 4.0，必须分开处理 | 只研究流程和风险；禁止复制其认证、上传、安全、支付实现。 |
| [dcloudio/uni-app](https://github.com/dcloudio/uni-app) | 官方页面注册、跨端编译、生命周期、请求和平台差异 | GitHub 页面标示 Apache-2.0，并提示仓库内可能有其他许可 | 采用官方 API/文档；不复制第三方示例资源。 |

## 研究字段矩阵

| 领域 | 参考项目共性 | CommerceFlow P6 决策 |
| --- | --- | --- |
| 商品列表/详情 | 商品卡、分类、价格、规格选择 | 使用本地真实 `ProductSummary`，保留名称、描述、分类、图片、SKU、价格、库存。 |
| SKU 选择 | 颜色/尺寸/数量影响可购买性 | 由 Java 返回的 `availableStock` 决定按钮状态；不在本地推断新库存。 |
| 购物车 | 通常由全局 store + API 同步 | Java/MySQL 为真相；本地只缓存展示状态和请求中的幂等信息。 |
| 订单 | 确认、提交、成功、列表、详情 | 只展示 `CREATED`；不做支付、物流、退款、地址、优惠券。 |
| 网络状态 | loading、empty、error、retry、图片失败 | P6B 每个核心页面必须有这些状态；状态来自真实请求。 |
| AI 入口 | 从商品详情携带 SKU 上下文进入问答 | P6C 调用新版 `/api/ai/customer-service/ask`，不调用旧 `product-chat`。 |

