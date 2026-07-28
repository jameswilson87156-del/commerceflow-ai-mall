# P6 移动端研究总结

## 结论速览

1. **当前完成度**：可构建的单页 UniApp/Vue3/Vite 原型；商品列表真实，SKU 选择和本地购物车可演示，订单提交是真 API，但没有完整移动商城流程。
2. **最大技术问题**：状态和请求没有分层；cart 是本地数组，AI 使用旧接口，Orders 是无效文本入口，HTTP 错误没有状态模型，localhost 不能支持真机。
3. **最终技术平台**：H5 作为主真实运行和截图平台；UniApp 多目标编译作为跨端证据；小程序/Android 只有在可复现真实运行后再宣称，暂不做 iOS 证据。
4. **最终页面范围**：商品列表、商品详情、SKU、购物车、订单确认/成功/列表/详情；P6C 加商品上下文 AI 客服。
5. **真实 API**：`GET /api/products`、`GET /api/products/{id}`、`GET/POST /api/cart`、`POST/GET /api/orders`、`GET /api/orders/{orderNo}`、新版 `POST /api/ai/customer-service/ask`。
6. **CORS**：当前 origin 是 Java 写死的 localhost 5173-5175；P6A 不改，P6 后续应改为白名单配置并继续暴露限流头。
7. **状态管理**：P6B 先用 Composition API composable；跨页面共享复杂后再评估 Pinia；服务端 cart/order 是唯一事实来源。
8. **可复用 Java 能力**：商品/SKU/库存读取、cart GET/POST、订单创建/列表/详情/幂等/库存不足、AI 新契约、Redis headers/fallback。
9. **最小后端增量**：优先为零；只有 cart update/delete 或确实阻塞移动链路时，才单独 ADR 评估。不要修改冻结订单/AI逻辑。
10. **Flyway**：P6A 不新增迁移；P6B 不应需要新表，图片和订单快照已有 V4/V7，AI 结构已有 V8。
11. **新图片**：不生成。复用仓库已有正式商品素材和 API 返回路径。
12. **参考图顺序**：先商品详情/SKU，再购物车/确认/成功，最后订单列表/详情；AI 从详情稳定后进入。
13. **禁止复制**：不复制 CRMEB、mall、mall-app-web、litemall 的整页、代码、SQL、认证、支付、上传、安全实现、Logo 或商品图。
14. **P6B 第一具体步骤**：先新增统一请求 wrapper、类型和图片 URL resolver，再把真实商品详情和服务端 cart 接入；不先做视觉填充。
15. **证据规则**：每个页面必须有 loading/empty/error/retry；截图只来自真实运行，build 只能作为编译证据。

## 研究来源

官方依据与开源项目许可证/来源见：

- `docs/reference/P6_UNIAPP_OFFICIAL_RESEARCH.md`
- `docs/reference/P6_MOBILE_REFERENCE_MATRIX.md`
- `docs/reference/P6_MOBILE_LICENSE_MATRIX.md`
- `docs/reference/P6_MOBILE_SOURCE_PROVENANCE.md`

