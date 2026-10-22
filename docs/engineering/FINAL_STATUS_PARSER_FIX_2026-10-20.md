# Showcase 状态解析补充记录（2026-10-20）

## 发现的问题

对已运行的本地 Showcase 进行独立复核时，`status.ps1` 曾返回退出码 1。Java、Python、Admin、Mobile HTTP 入口均返回 200，但依赖健康状态读取失败。

根因是 Windows PowerShell 5.1 通过原生命令管道读取 `docker compose ps --format json` 时，完整 JSON 的 `Command` 字段包含非 ASCII/转义引号内容；本机控制台代码页解码后出现损坏字符，导致 `ConvertFrom-Json` 报错。该问题说明先前健康回归通过并不足以证明完整状态命令始终可用。

## 修复内容

文件：`scripts/showcase/showcase-common.ps1`

`Get-ShowcaseComposeServices` 改为通过 Docker Compose Go template 只输出本流程真正需要的字段：`Service|State|Health|ID|Name`，然后构造 PowerShell 对象。这样不再将包含命令行和其他 Unicode 元数据的完整 JSON 经过 Windows PowerShell 5.1 的本机输出解码器。

保留现有依赖健康分类和 fail-closed 检查；不跳过健康检查，也不改变容器状态的判定规则。保留 UTF-8 BOM，确保 Windows PowerShell 5.1 可以解析脚本中的中文字符串。

## 本轮实际验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| Windows PowerShell 5.1 Compose 健康回归（含 Live） | PASS | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/showcase/tests/verify-compose-health.ps1 -Live`；16 项断言通过，退出码 0。 |
| `scripts/showcase/status.ps1` | PASS | Windows PowerShell 5.1 实际执行，退出码 0；MySQL/Redis 为 running/healthy，Flyway V12，API/readiness/Python/Admin/Mobile 均报告 200。 |
| HTTP 探测 | PASS | `http://127.0.0.1:15174`、`http://127.0.0.1:15173`、`http://127.0.0.1:18081/actuator/health`、`http://127.0.0.1:18082/health` 均返回 200。 |
| `git diff --check` | PASS | 退出码 0。Git 对 LF/CRLF 给出提示，无 whitespace error。 |
| PowerShell 7 验证 | NOT_RUN | 本机当前未找到 `pwsh` 命令。 |
| 完整浏览器 E2E | BLOCKED | Tabbit Browser 路由不可用；本补充轮次未切换其他浏览器后端。 |
| 真实 DeepSeek 调用 | NOT_RUN | 仍使用本地 MOCK Provider，未发起外部模型请求。 |

## 当前边界

这次修复确认本地 Showcase 状态检查和服务健康端点恢复正常，但不代表完整浏览器 E2E 已通过。服务使用隔离本地环境与 MOCK AI；没有生产数据库、真实支付、云部署或性能压测结论。

## Git 与安全状态

工作区：`D:\面试冲刺包\_execution-20261020\commerceflow-ai-mall-git`
分支：`fix/timeout-cancellation-showcase-20261020`
基线 HEAD：`b9bdaa1e8bb5a89735403fe66c55413d2628905b`

本轮未提交、推送、合并或部署。原归档和旧候选快照未修改。