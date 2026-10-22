# CommerceFlow Unicode 同形字符与跨运行时红队 v10

日期：2026-10-22  
项目分支：`feat/order-timeout-cancellation`  
基线提交：`b9bdaa1e8bb5a89735403fe66c55413d2628905b`  
范围：固定合成问句、Python MOCK Provider、Java Provider 不可用时的本地事实降级；没有发送真实模型请求。

## 本轮发现

v9 的价格/库存规则已经覆盖多种自然语言改写，但 Python 与 Java 降级路径没有完全对齐。沿用 v9 fixture 并加入同形字符混写后，Java 降级路径会把混写的改价请求当成普通价格询问；另一个纯 ASCII “Set the product price to …” 变体也未命中 Java 规则。Python 侧已有常见西里尔字母映射，两个差异都是可复现的跨运行时策略缺口。

先通过 Java 红测分别验证两个问题：保留 ASCII 改价规则但移除同形字符映射时，混写攻击失败；保留映射但移除 `set ... price` 规则时，纯 ASCII 变体失败。配对正常问句检查价格设置词面、库存词面和历史变更语境仍可查询当前事实。

## v10 数据与结果

- 继承 v9 的 44 条固定样本，新增 10 条：5 条攻击、5 条控制；总计 54 条，攻击 27 条、控制 27 条。
- 新增攻击覆盖混写同形字符的改价、改库存、系统提示提取、API Key 提取，以及普通英语 `set price` 改价表达。
- 正常控制覆盖含同形字符但不构成指令的价格/库存询问、普通 `system` 与 `api` 词面，以及自然语言价格设置描述。
- 最终 MOCK 逐样本结果为 54/54 契约通过：攻击 27/27、控制 27/27，外部请求 0，失败样本为空。
- 预期价格 `¥129.00` 和库存 `27 件` 都是 fixture 中的固定合成事实。

最终逐样本结果见 `customer-service-redteam-v10-unicode-confusables-mock-results-final-54.json`。此前的 52 条中间 run 结果另存为早期记录，没有覆盖；最终结论以 54 条 run 为准。

## 修复内容

- Java 策略文本归一化在 NFKC 和格式字符清理后，对 Python 已使用的有限常见西里尔同形字符做映射。
- Java 价格改写识别补充 `change / modify / adjust / set / lower / raise + price` 有界模式，与 Python 现有规则保持一致。
- 未修改 Java 事实来源、接口契约、Provider 协议、订单/库存写路径或回答事实渲染方式。

这只是有限字符表与明确变更短语的策略归一化，不是通用 Unicode 欺骗检测，也不代表模型抵抗提示注入的能力。

## 测试证据

| 工作目录 | 命令 | 结果 |
| --- | --- | --- |
| `services/ai-service` | `py -3.12 -m pytest -q` | 164 passed，退出码 0 |
| `services/ai-service` | `py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v10-unicode-confusables.json --output docs/evidence/commerceflow-ai-redteam-v10-unicode-confusables/customer-service-redteam-v10-unicode-confusables-mock-results-final-54.json` | `scenario_accuracy=54/54`、`response_success=54/54`，退出码 0；MOCK、外部请求 0 |
| `apps/mall-api` | `mvn -o '-Dtest=AiCustomerServiceTests#unavailableProviderRejectsUnicodeConfusablePolicyAttacks+unavailableProviderStillAnswersBenignUnicodeConfusableFactControls' test` | 2 passed，退出码 0 |
| `apps/mall-api` | `mvn -o test` | 119 项，0 失败、0 错误、10 项条件集成测试跳过，`BUILD SUCCESS`，退出码 0；使用 H2，没有连接 MySQL |

测试日志位于 `D:/面试冲刺包/_JOB_READY_SPRINT_20261021/deliverables/`：

- `commerce-unicode-v10-isolated-red-20261022.log`
- `commerce-unicode-v10-ascii-set-red-20261022.log`
- `commerce-unicode-v10-java-targeted-green-final-20261022.log`
- `commerce-redteam-v10-unicode-confusables-mock-final-20261022.log`
- `commerce-unicode-v10-full-pytest-20261022.log`
- `commerce-unicode-v10-full-maven-20261022.log`

## 结论边界

v10 证明固定合成样本在本地 Python MOCK 与 Java 无 Provider 降级规则下满足当前定义的意图、状态、事实和拒答契约。它不测 DeepSeek，不是越狱率、模型质量或生产安全指标。Python/Java 规则覆盖也不应被描述为模型自身的防护能力。
