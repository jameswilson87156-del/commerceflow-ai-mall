# 项目 B 最终复核补充（2026-10-20）

## 结论

正确的目标提交已在新隔离 Git checkout 中恢复。订单超时取消事务隔离、MySQL 专项测试、前后端构建/测试、Python 测试、本地 Showcase API smoke 均为 **PASS**。本地 Showcase 服务正在运行，使用 MOCK Provider。浏览器端到端为 **BLOCKED**，因为本机没有可路由的 Tabbit Browser 安装。

## 仓库、分支与原始工作区保护

- 目标 checkout：`<LOCAL_PATH>`
- 目标远端分支：`feat/order-timeout-cancellation`
- 目标 SHA、`origin/feat/order-timeout-cancellation` 与本地 `HEAD` 均为 `b9bdaa1e8bb5a89735403fe66c55413d2628905b`。
- 为承载未提交修改而建立的本地分支：`fix/timeout-cancellation-showcase-20261020`；当前修改未暂存、未提交。
- 旧候选目录 `<LOCAL_PATH>`、`<LOCAL_PATH>`、`<LOCAL_PATH>` 未被写入、覆盖或重建。旧阻塞记录指出前两个 worktree 指针失效，第三个缺少有效 Git 元数据；它们没有被冒充为目标 checkout。
- 旧 `commerceflow-showcase` MySQL 数据卷保留；本轮使用独立 Compose 项目和独立数据卷，没有执行 `down -v`。
- `.env` 仍被 Git 忽略，本轮未修改或输出其中的凭据。没有提交、推送、合并、PR 或部署。

## 代码改动

- 订单超时取消改为逐订单独立事务处理；新增 `OrderTimeoutCancellationProcessor`，使单笔失败回滚自己的订单状态、库存回补与 outbox 变更，并允许批次继续处理其他订单。
- 新增 MySQL 订单可靠性与超时取消集成测试，覆盖库存、证据、outbox、失败回滚/重试及并发取消。
- Showcase JSON Lines Compose 解析、健康状态分类、启动清理归属判断与外部命令退出码检查已补齐。
- Showcase 项目名恢复顺序现为显式环境配置优先、已有 `.showcase/processes.json` 项目名其次、默认值最后；正常运行 `status.ps1` / `stop.ps1` 不再依赖重复手动传入临时项目名。
- `verify.ps1` 的 4xx/429 读取兼容 PowerShell 7 的 `-SkipHttpErrorCheck` 和 Windows PowerShell 5.1 的 WebResponse 路径。

修改文件：

- `apps/mall-api/src/main/java/com/commerceflow/mall/order/timeout/OrderTimeoutCancellationService.java`
- `apps/mall-api/src/main/java/com/commerceflow/mall/order/timeout/OrderTimeoutCancellationProcessor.java`（新增）
- `apps/mall-api/src/test/java/com/commerceflow/mall/order/timeout/OrderTimeoutCancellationServiceTests.java`
- `apps/mall-api/src/test/java/com/commerceflow/mall/OrderTimeoutCancellationMySqlTests.java`（新增）
- `scripts/showcase/showcase-common.ps1`
- `scripts/showcase/start.ps1`
- `scripts/showcase/status.ps1`
- `scripts/showcase/verify.ps1`
- `scripts/showcase/tests/verify-compose-health.ps1`（新增）

## 测试与运行证据

| 检查 | 状态 | 命令、工作目录与结果 |
| --- | --- | --- |
| Java 完整套件（环境门控集成项关闭） | PASS | 工作目录目标 checkout 根目录；`mvn.cmd -f apps/mall-api/pom.xml test`；退出码 0；120 项，0 失败、0 错误、12 跳过。 |
| 隔离 MySQL 专项 | PASS | 同上工作目录；`mvn.cmd -f apps/mall-api/pom.xml '-Dtest=OrderReliabilityMySqlTests,OrderTimeoutCancellationMySqlTests' test`，启用对应 MySQL 测试门控；退出码 0；7 项，0 失败、0 错误、0 跳过。使用 Docker MySQL 8.4.10 和隔离 schema `commerceflow_order_reliability_20261020`，不是生产数据库。 |
| Admin 依赖安装 | PASS | 工作目录 `...\apps\admin-web`；`npm.cmd ci`；退出码 0，165 个包安装完成。 |
| Admin 单元测试 | PASS | 工作目录 `...\apps\admin-web`；`npm.cmd test`；退出码 0；46 项通过。 |
| Admin 生产构建 | PASS | 同上；`npm.cmd run build`；退出码 0；Vue 类型检查与 Vite build 通过。 |
| UniApp H5 依赖安装 | PASS | 工作目录 `...\apps\mobile-app`；`npm.cmd ci --legacy-peer-deps`；退出码 0，450 个包安装完成。 |
| UniApp H5 单元测试 | PASS | 工作目录 `...\apps\mobile-app`；`npm.cmd test`；退出码 0；36 项通过。 |
| UniApp H5 构建 | PASS | 同上；`npm.cmd run build`；退出码 0；Vite H5 build 通过，输出有依赖弃用/`NODE_ENV` 提示。 |
| AI Python 服务 | PASS | 工作目录 `...\services\ai-service`；`py -3 -m pytest -q`；退出码 0；53 项通过。 |
| Compose 健康解析回归 | PASS | Windows PowerShell 7.6.5 与 Windows PowerShell 5.1 均运行 `scripts/showcase/tests/verify-compose-health.ps1 -Live`；两次退出码 0；各 16 项断言通过，含实际 Compose MySQL/Redis 健康检查。 |
| Showcase API smoke | PASS | 工作目录目标 checkout 根目录；`scripts/showcase/verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke`；Windows PowerShell 7.6.5、Windows PowerShell 5.1 两次退出码均为 0。订单创建与同 Key 重放返回同一订单；同 Key 不同请求返回 409；限流为 5 次成功、随后 1 次 429。脚本还检查 MOCK AI 的 Evidence/Trace、限流响应头、商品图片及 H5 可达性。 |
| 当前服务状态 | PASS | `scripts/showcase/status.ps1` 退出码 0；API/readiness、Python、Admin、Mobile HTTP 均为 200；隔离 MySQL 与 Redis healthy；Flyway `V12`。 |
| `git diff --check` | PASS | 目标 checkout 根目录；退出码 0。仅有 Git 的 CRLF/LF 提示。 |
| 浏览器端到端 | BLOCKED | 用户已授权启动一次 Tabbit Browser。稳定启动器诊断退出码 69；当前用户 Tabbit 应用注册列表无规范匹配，`%LOCALAPPDATA%\Tabbit` 下未找到 Browser 主程序，managed 实例目录为空。没有使用其他浏览器后端。 |
| DeepSeek | NOT_RUN | Showcase Provider 明确为 `commerceflow-mock / MOCK`；没有真实 DeepSeek 请求。 |

MySQL 测试日志与隔离 schema 脚本：`<LOCAL_PATH>`。最终 smoke 输出：`<LOCAL_PATH>`。

表格中的目标 checkout 根目录完整路径为 `<LOCAL_PATH>`；Admin、Mobile H5 与 Python 测试工作目录分别为该路径下的 `apps\admin-web`、`apps\mobile-app` 和 `services\ai-service`。MySQL 专项使用 `ORDER_RELIABILITY_MYSQL_ENABLED=true`，`ORDER_RELIABILITY_DB_URL` 指向上述隔离 schema；凭据未写入报告。

## 当前本地演示状态

- Admin：`http://127.0.0.1:15174`
- Mobile H5：`http://127.0.0.1:15173`
- Java API：`http://127.0.0.1:18081`
- Python AI 服务：`http://127.0.0.1:18082`
- MySQL / Redis 仅用于本地隔离演示和测试，端口分别为 `13307` / `16381`；独立 Compose 项目为 `commerceflow-showcase-closeout-20261020`，数据卷保留。

## 面试可讲范围与限制

可基于代码和测试讲逐订单事务隔离、失败订单回滚、库存与 outbox 一致性、重复请求幂等和本地 MOCK AI 的 Evidence/Trace。不能把 MOCK 结果说成 DeepSeek 效果，也不能声称已完成浏览器 E2E、云部署、生产库验证或性能压测。
