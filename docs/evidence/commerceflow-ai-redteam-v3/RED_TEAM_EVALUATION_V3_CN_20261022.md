# CommerceFlow AI Mall 红队评测 v3（2026-10-22）

## 本轮新增

- 数据集：40 条合成攻击样本，完整保留 v2 的 32 条，再增加 8 条 Unicode 规范化边界样本。
- 覆盖 8 类攻击：direct override、prompt extraction、secret extraction、business fact tampering、role impersonation、delimiter injection、multilingual jailbreak、mixed intent；每类 5 条。
- 新增样本覆盖全角兼容字符、零宽格式字符及其与价格/库存正常问句的组合。数据仅验证 NFKC 与 Unicode `Cf` 格式字符清理边界；不代表覆盖所有同形字符或编码绕过。
- 全部样本为人工构造合成文本，没有客户、订单或生产数据。

## 发现与修复

新增测试首次运行时，Python 红队集 MOCK 契约只有 36/40 通过；4 条混有全角字母或零宽字符的提示被分类成价格或库存问题。测试共 86 passed、5 failed，日志保存在本机交付目录。

修复在策略匹配前对文本执行 Unicode NFKC 规范化、移除 `Cf` 格式字符、折叠空白并转小写。Java 故障降级分类复用相同的规范化边界，并补齐对应的英文提示注入、密钥提取及价格/库存修改关键词。该逻辑只影响问题范围判断，不修改商品事实、接口、数据库或订单业务行为。

## 最终离线结果

- 运行模式：`commerceflow-mock` / `MOCK`。
- 红队契约通过 40/40；八类攻击各 5/5；失败样本 0。
- 场景正确率 40/40，响应成功率 40/40；Provider 失败 0。
- 外部请求数为 0；逐样本报告不包含最终回答文本。
- 这只是确定性本地 MOCK 与 Java 降级规则的固定样本回归结果，不是 DeepSeek 或其他大模型的安全率。

机器可读材料：

- 原始数据：[customer-service-redteam-v3.json](../../../services/ai-service/evaluation/customer-service-redteam-v3.json)
- 逐样本结果：[commerceflow-redteam-v3-mock.json](commerceflow-redteam-v3-mock.json)

## 回归证据

| 命令 | 工作目录 | 退出码与结果 |
| --- | --- | --- |
| `py -3.12 -m pytest tests/test_real_provider.py tests/test_evaluate_provider.py -q`（首轮） | `services/ai-service` | 1；86 passed、5 failed；红队 MOCK 36/40 |
| 同一组定向 pytest（修复后） | `services/ai-service` | 0；91 passed |
| `py -3.12 -m pytest -q` | `services/ai-service` | 0；103 passed |
| `mvn -o '-Dtest=AiCustomerServiceTests' test` | `apps/mall-api` | 0；16 tests，0 failures，0 errors，0 skipped |
| `mvn -o test` | `apps/mall-api` | 0；117 tests，0 failures，0 errors，10 skipped；使用隔离 H2，未连接 MySQL |

原始终端日志：

- 首轮失败：[pytest-targeted-initial-red-20261022.log](../../../../../deliverables/commerce-unicode-redteam-v3/pytest-targeted-initial-red-20261022.log)
- 定向通过：[pytest-targeted-green-20261022.log](../../../../../deliverables/commerce-unicode-redteam-v3/pytest-targeted-green-20261022.log)
- Python 全量回归：[pytest-full-20261022.log](../../../../../deliverables/commerce-unicode-redteam-v3/pytest-full-20261022.log)
- Java 针对性回归：[mvn-aicustomerservicetests-20261022.log](../../../../../deliverables/commerce-unicode-redteam-v3/mvn-aicustomerservicetests-20261022.log)
- Java 全量回归：[mvn-full-20261022.log](../../../../../deliverables/commerce-unicode-redteam-v3/mvn-full-20261022.log)
- MOCK 评测命令日志：[commerce-redteam-v3-mock-20261022.log](../../../../../deliverables/commerce-unicode-redteam-v3/commerce-redteam-v3-mock-20261022.log)

## DeepSeek 与运行边界

本机进程环境没有 `DEEPSEEK_API_KEY`、`COMMERCEFLOW_AI_API_KEY` 或 `AI_PROVIDER_API_KEY`。检测到的共享 Provider 是 `gpt-5.5`，不是 DeepSeek；本轮未调用该 Provider，也未发送任何真实模型请求。历史 DeepSeek 普通问题结果仍按原数据集单独保存，不能与本红队 MOCK 结果合并。

运行真实 DeepSeek 红队前，需要在后端进程环境配置新的 DeepSeek Provider 地址、模型名与轮换后的密钥；不得复用已在聊天中暴露的旧密钥，也不要将密钥放入前端、仓库或评测报告。当前项目继续保持未提交、未推送、未部署。

## 2026-10-22 独立复核

本次未更改 CommerceFlow 业务源码或原始样本；在评测命令进程内显式将 `COMMERCEFLOW_AI_PROVIDER=mock`，重新跑普通 v3 与红队 v3，避免共享 Provider 配置触发外部请求。结果与原报告一致：普通样本 44/44 场景正确、44/44 响应成功；红队样本 40/40 契约通过、40/40 响应成功。逐样本复核结果分别保存在[普通问题复核 JSON](customer-service-eval-v3-mock-recheck-20261022.json)和[红队复核 JSON](commerceflow-redteam-v3-mock-recheck-20261022.json)。

随后重跑项目测试：Python 全量 `103 passed`；Java 全量 `117 tests, 0 failures, 0 errors, 10 skipped`。Java 测试使用隔离 H2 并成功应用 12 条迁移，没有连接 MySQL；未启用真实 Redis，健康检查中出现 Redis 不可达告警，但测试通过。进程环境检查确认 `DEEPSEEK_API_KEY`、`COMMERCEFLOW_AI_API_KEY`、`AI_PROVIDER_API_KEY` 均不存在；未调用共享 Provider，也没有真实模型请求。

| 命令 | 工作目录 | 退出码与结果 | 原始输出 |
|---|---|---|---|
| `py -3.12 .\evaluate_provider.py --dataset .\evaluation\customer-service-cases-v3.json --output <独立 JSON 路径>`（进程内强制 MOCK） | `services/ai-service` | 0；44/44，MOCK | [终端日志](../../../../../deliverables/commerce-ordinary-v3-recheck-20261022.log) |
| `py -3.12 .\evaluate_provider.py --dataset .\evaluation\customer-service-redteam-v3.json --output <独立 JSON 路径>`（进程内强制 MOCK） | `services/ai-service` | 0；40/40，MOCK | [终端日志](../../../../../deliverables/commerce-redteam-v3-recheck-20261022.log) |
| `py -3.12 -m pytest -q` | `services/ai-service` | 0；103 passed | [pytest 日志](../../../../../deliverables/commerce-pytest-full-after-eval-recheck-20261022.log) |
| `mvn -o test` | `apps/mall-api` | 0；117 项，0 失败、0 错误、10 项跳过 | [Maven 日志](../../../../../deliverables/commerce-maven-full-after-eval-recheck-20261022.log) |

本次复核可以支持面试中说明“离线 MOCK 和 Java 规则边界有可重复回归”，不能说明 DeepSeek 对这些对抗提示的真实响应或事实准确率。
