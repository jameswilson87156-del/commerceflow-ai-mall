# P6 项目集成审查

## 地址与环境

当前移动页在 `index.vue` 第 3 行以 `VITE_API_BASE` 为优先、`http://localhost:8080/api` 为默认值。这个默认值只适用于 Windows 上的 H5 浏览器；真实 Android/iOS 设备中的 localhost 指向设备自身。不能把开发机 LAN IP 写死进仓库。

建议分层：

| 场景 | 地址来源 |
| --- | --- |
| Windows H5 | `.env.local` 的 `VITE_API_BASE`，或同源反代 |
| Android 模拟器 | 专用开发地址，例如模拟器访问宿主机的约定地址，由运行脚本注入 |
| 局域网真机 | 启动时注入可达的局域网地址；不提交真实 IP |
| 预发布/部署 | CI 或部署环境变量 |

## CORS

`apps/mall-api/src/main/java/com/commerceflow/mall/core/WebConfig.java` 当前把 localhost/127.0.0.1 的 5173-5175 端口写死，并已暴露 P5 限流头。P6A 不修改它。后续应把允许 origin 配置化，保留明确白名单，禁止 `allowedOrigins("*")`；H5 需要 CORS，原生 App 的请求不等同于浏览器 CORS，但同一 API 地址仍需可达。

## 图片路径

Java 返回 `/assets/products/...` 的本地公共资源路径。移动端必须通过集中式 `resolveImageUrl()` 拼接 API origin，处理空值、相对路径和加载失败；不能在每个组件里拼接 URL，也不能使用网络图片或字母占位图。

## 用户与状态

当前所有演示 API 默认 `userId=1`，账号数据是 Flyway 的单个 Demo Buyer。P6 可以继续使用演示用户，但应把它放进 demo session/config，而不是散落在请求字符串中。购物车真相必须来自 Java/MySQL；本地存储最多保存 UI 恢复信息和当前幂等 Key，不能本地伪造订单成功。

## 全局错误

当前 `request()` 直接 `response.json()`，不检查 HTTP status，不区分网络错误、空数据、库存不足、409、429 和 5xx。P6B 要求统一 wrapper 返回 typed result，并允许页面区分 loading/empty/error/retry；P6C 复用同一 wrapper 解析 429、Retry-After、FAIL_OPEN。

## CI 与文档陈旧项

CI 当前移动 job 只运行 `npm run build` 和 `npm run build:uni`，没有移动测试、真实 API 合约、设备验收或 screenshot artifact。根目录既有移动截图中 `mobile-orders.jpg` 不能自动证明当前源码已有订单页，因为当前源码只有一个没有点击处理的 Orders 文本。历史 README 的“verified”需要在每个 P6 阶段用新命令和新证据重新确认。

## 未使用依赖与占位

移动工程没有 Pinia、HTTP 客户端、测试框架或组件目录；这是未实现，不应通过添加依赖掩盖业务缺口。P6B 先用原生 `uni.request` 封装最小 wrapper；只有状态跨页面确实变复杂，才评估 Pinia。

