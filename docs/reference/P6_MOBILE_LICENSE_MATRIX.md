# P6 移动端许可证矩阵

| 来源 | 公开许可证状态 | 允许研究 | 明确禁止 |
| --- | --- | --- | --- |
| `dcloudio/uni-app` | GitHub 页面标示 Apache-2.0，并提示仓库内存在其他许可文件 | 官方框架 API、文档、平台编译规则 | 复制官方示例业务页、图片、Logo 到 CommerceFlow |
| `crmeb/crmeb_java` | GitHub 页面标示 Apache-2.0 | 页面流转、模块命名、API 分层思路 | 复制商品/订单整页、认证、支付、上传、数据库或品牌素材 |
| `macrozheng/mall` | GitHub 页面公开 Apache License 2.0 | 商品/SKU/购物车/订单领域概念和目录阅读 | 复制 Java/MyBatis/Redis 实现、页面、素材或 SQL |
| `macrozheng/mall-app-web` | GitHub 页面公开 Apache License 2.0 | UniApp Vue3 的 `apis`、`pages`、`stores`、`types`、`utils` 组织思路 | 复制组件、CSS、图片、完整页面或接口实现 |
| `linlinjava/litemall` | GitHub 页面标示 MIT；文档另有 CC BY-ND 4.0 | 仅阅读流程、空状态和风险边界 | 复制代码、认证、上传、安全、支付；文档/图片不可混同代码许可证 |

## 统一合规规则

1. 本轮不克隆外部仓库，不复制任何外部源码。
2. Apache/MIT 只代表对应仓库在其许可证范围内可研究或按条件复用，不能覆盖第三方依赖、图片、字体、Logo 或文档的独立权利。
3. GPL、未知许可证或无法确认来源的内容只允许作为视觉/流程研究，不进入 CommerceFlow 代码。
4. `litemall` 的认证、上传和安全代码不进入 CommerceFlow；CRMEB 的老旧依赖和全量业务不进入 CommerceFlow。
5. 如果未来确实复用少量代码，必须在提交前记录文件、版本、许可证、修改方式和 `THIRD_PARTY_NOTICES.md`；P6A 不产生此类复用。

