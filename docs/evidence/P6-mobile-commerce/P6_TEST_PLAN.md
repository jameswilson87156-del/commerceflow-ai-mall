# P6 移动端测试计划

## 单元/组件测试

P6B 实现后必须覆盖：

1. 商品列表成功、loading、empty、error、retry。
2. 商品详情真实 API、商品切换、SKU 颜色/尺寸选择、图片路径解析和图片失败。
3. 可购买状态、正常库存、低库存、缺货禁用按钮。
4. Cart 服务端读取、增加、数量变化边界、空购物车和请求失败。
5. 订单确认金额使用 BigDecimal 字符串/服务端返回，不能使用浮点计算作为事实。
6. `Idempotency-Key` 首次提交、网络重试复用、相同 Key 重放、不同请求体 409。
7. 库存不足时不显示成功订单，不清理未确认购物车。
8. 下单成功、订单列表、订单详情、空列表和重试。

P6C 实现后必须覆盖：

1. 从商品详情携带 productId/skuId 进入 AI。
2. loading、Mock 正常回答、fallback、服务不可用。
3. 429 读取 Retry-After、按钮禁用、倒计时归零和重试。
4. FAIL_OPEN 显示 degraded，不显示虚假 remaining。
5. Evidence/Trace 只显示当前真实响应。

## 浏览器验收

- H5 viewport：390x844、430x932；DPR 1 和一个高 DPR 检查。
- 触摸点击 SKU、数量、确认、重试；检查浏览器返回/页面返回。
- 检查状态栏和底部安全区；没有水平滚动；长文本换行。
- 控制台错误 0；商品和订单图片 HTTP 200；失败图片显示真实错误状态。
- 真实 MySQL、Java API、P6C 需要时启动 Python/Redis；不使用外网或真实 API Key。

## 构建与回归

```text
apps/mobile-app: npm test                 # P6B 新增测试脚本后执行
apps/mobile-app: npm run build            # H5
apps/mobile-app: npm run build:uni        # UniApp 编译
apps/admin-web: 现有 Vue 测试与 build     # 防止共享 API 契约漂移
apps/mall-api: Maven 完整测试              # 订单/AI/Redis 回归
services/ai-service: pytest                # P4/P5 Python 回归
```

构建成功只证明编译，不证明真实设备适配；真机证据需要单独记录设备、版本、地址、截图和控制台/网络结果。

