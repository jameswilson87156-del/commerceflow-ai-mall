# P6 移动端 API Runtime 设计

## 地址来源

禁止在页面组件散落 `http://localhost:8080`。统一配置 `VITE_API_BASE` 或等价运行时注入值：

- H5 本地：浏览器可达的 Java `/api` 地址或 H5 dev proxy。
- H5 预发布：部署环境注入的 API origin。
- Android 模拟器：按模拟器到宿主机的可达地址配置。
- LAN 真机：启动脚本或 `.env.local` 注入，不提交 LAN IP。

手机访问 `localhost` 时访问的是手机本身，不是 Windows 主机；真机还需要防火墙、监听地址、同网段和 HTTPS/证书条件。

## Wrapper 责任

统一 wrapper 必须：

1. 接受路径、method、body、headers、超时和可取消任务。
2. 统一拼接 API base 和编码 query。
3. 检查 HTTP status，不把 4xx/5xx 当成正常 JSON。
4. 返回 typed data、status、headers、业务 code 和可读错误。
5. 解析 `X-RateLimit-Mode`、`X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset`、`Retry-After`。
6. 对 429 返回可重试时间；对网络取消区分“用户取消”和“网络失败”。
7. 对图片使用同一 `resolveImageUrl`，处理相对路径、空路径、加载失败。

## 业务约束

- cart 的事实来自 Java/MySQL；本地缓存不能产生订单。
- 订单 POST 的 `Idempotency-Key` 在点击提交前生成，在网络重试时复用；成功或明确冲突后才清理。
- 不能通过重试生成第二个订单。
- AI 入口必须传真实商品/SKU上下文，不能使用固定 SKU。
- AI 429 只能展示倒计时和重试状态，不创建 Evidence/Trace 假记录。

