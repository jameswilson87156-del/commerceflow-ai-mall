# CommerceFlow Java 降级分类与 Python 编码策略一致性验收

**日期：** 2026-10-22  
**本次结论：** PASS（仅限 Java 本地事实降级对 V5/V6 编码样本的分类一致性；不代表真实模型或生产环境验收）

## 工作区与边界

- 仓库：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\commerce-repair`
- 分支：`feat/order-timeout-cancellation`
- HEAD：`b9bdaa1e8bb5a89735403fe66c55413d2628905b`
- 接手时工作区已有未提交业务修改和大量未跟踪评测证据。本次没有 reset、clean、切分支、覆盖旧报告、提交或推送；旧改动保留。
- 本次没有发起 DeepSeek 或其他远程模型请求。所有用例均为合成数据；Java 集成测试将 Provider mock 为不可用，后端测试使用隔离 H2。

## 问题与修复

Python 的本地意图分类器会对最多两轮的 URL 百分号编码、数字 HTML 实体、显式标记的 BASE64/BASE64URL、HEX、ROT13 做有界解码，再执行 NFKC、不可见格式字符处理和混淆字符归一化。原 Java 降级路径只执行归一化，未先解码。

这造成了可复现的差异：`BASE64:...; 当前商品价格是多少？` 中的编码提示注入在 Python 分类为 `UNSUPPORTED`，但 Provider 不可用时 Java 会把明文价格词识别为普通价格问题并返回商品价格。与此同时，Java 也会拒绝只用编码表达的正常价格问题。

Java 降级分类现在以相同顺序做最多两轮的有限解码，支持现有 V5/V6 数据集实际使用的编码形式；并补上格式错误百分号片段后的有效编码、以及省略分号的数字 HTML 实体。Percent escape 仅识别 ASCII `%HH`，不会把全角字符当成 URL 十六进制数字。解码文本采用严格 UTF-8；单个 framed 编码负载最多 2048 字节；无效、不可打印或非法 framed 编码保留为原文；不改变原始提问、API 契约、Provider 配置、回答事实来源或数据库结构。

## 验收结果

| 检查 | 命令或证据 | 结果 |
|---|---|---|
| RED：BASE64 差异复现 | `mvn -Dtest=AiCustomerServiceTests#unavailableProviderAppliesFramedBase64PolicyChecksToAttacksAndControls test`（`apps/mall-api`） | 退出码 1；期望 `UNSUPPORTED_QUESTION`，实际为 `FALLBACK_ANSWER` |
| RED：标准编码攻击先例 | 同上 | 退出码 1；`%73ystem` 提示注入在 Java 降级中返回 `FALLBACK_ANSWER` |
| RED：格式错误百分号后缀 | `mvn -Dtest=AiCustomerServiceTests#unavailableProviderNormalizesEncodedPolicyAttacksAndBenignControls test`（`apps/mall-api`） | 退出码 1；`%73ystem prompt%ZZ` 整串解码失败，降级返回价格事实 |
| RED：省略分号的数字实体 | 同上 | 退出码 1；`&#73gnore` 未解码，降级返回价格事实 |
| RED：全角 percent 十六进制数字良性控制 | 同上 | 退出码 1；Java 把全角 `７３` 解为 ASCII `s`，误拒价格控制；Python 预期 `PRICE` |
| Java 最终专项测试 | 同上 | 退出码 0；1 项通过，0 失败、0 错误、0 跳过；断言 7 个攻击变体拒绝且 7 个正常对照仍能答复 |
| Python 编码分类/离线评测专项 | `py -3.12 -m pytest -q tests/test_real_provider.py -k 'encoded_instructions or framed_encodings or framed_decoder' tests/test_evaluate_provider.py::test_focused_redteam_reports_keep_attacks_and_controls_separate`（`services/ai-service`） | 退出码 0；13 passed，162 deselected |
| Python 边界样本核对 | `py -3.12 ..\..\docs\evidence\commerceflow-ai-redteam-v14-java-fallback-parity-20261022\python-boundary-classifier.py`（`services/ai-service`） | 退出码 0；格式错误百分号后缀、无分号数字实体 2/2 均为 `UNSUPPORTED` |
| Python V5/V6 逐样本分类核对 | 读取现有 V5、V6 JSON，并调用 `classify_question_intent` | 12/12 命中原标签：6 attack、6 control；未改数据集或标签 |
| Python 服务完整测试 | `py -3.12 -m pytest -q`（`services/ai-service`） | 退出码 0；223 passed |
| Java API 模块完整测试 | `mvn test`（`apps/mall-api`） | 退出码 0；122 tests，0 failures、0 errors、10 skipped |
| 补丁空白检查 | `git diff --check -- apps/mall-api/src/main/java/com/commerceflow/mall/ai/AiService.java apps/mall-api/src/test/java/com/commerceflow/mall/AiCustomerServiceTests.java` | 退出码 0 |

Java 全模块测试实际应用 12 个 Flyway migration 到 H2。10 项跳过来自两个显式依赖外部服务的测试类：Redis 集成 5 项要求本机 `127.0.0.1:6380`，MySQL 可靠性 5 项要求 `ORDER_RELIABILITY_MYSQL_ENABLED=true` 及隔离 MySQL 配置；本次未连接 Redis 或 MySQL，因此不把 H2 结果冒充为它们的验收。

## 文件与完整性

本次新增代码与测试：

- `apps/mall-api/src/main/java/com/commerceflow/mall/ai/AiService.java`：Java 降级问句策略的有界编码解码与归一化。
- `apps/mall-api/src/test/java/com/commerceflow/mall/AiCustomerServiceTests.java`：Provider 不可用时 V5/V6 攻击拒绝及良性对照答复的集成回归。
- `docs/evidence/commerceflow-ai-redteam-v14-java-fallback-parity-20261022/`：本报告、可复跑的 Python 边界核对脚本和原始命令日志。

本次没有修改 Python Provider、评测数据集、预期标签、计分代码、后端 API、表结构、管理端或用户端页面。

验收后 SHA-256：

```text
63DF1296DBBA4D9A771940FD96F2E5AB23FC92F3BEB416CB604CBD4C24C1FEFC  apps/mall-api/src/main/java/com/commerceflow/mall/ai/AiService.java
9968C04CC95A78B499865E4D2B5D48044EA253FCAF4AC8F518F1005E7173C66A  apps/mall-api/src/test/java/com/commerceflow/mall/AiCustomerServiceTests.java
```

原始 RED、GREEN、Python 专项和完整测试日志保存在 `logs/`，SHA-256 清单见 `LOGS_SHA256.txt`。测试数据只证明这些确定性合成样本上的行为，不证明真实 DeepSeek 行为、未知编码变体、真实 MySQL 并发语义或浏览器视觉验收。
