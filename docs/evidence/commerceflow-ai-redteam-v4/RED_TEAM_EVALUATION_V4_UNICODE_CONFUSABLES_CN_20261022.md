# CommerceFlow AI Mall 红队 v4：跨文字系统同形字（2026-10-22）

本评测使用 4 条人工构造的合成问题：2 条在“system prompt”和“price”指令中混入西里尔同形字，另有 2 条包含相同字符的普通价格问题作控制。没有真实客户、订单或生产数据。

## 复现与修复

在现有 NFKC 与 Unicode `Cf` 格式字符清理后追加探测时，以下问题仍被本地意图分类器识别为 `PRICE`：将 “system” 的 `s` 替换为西里尔 `ѕ`；将 “price” 的 `i` 替换为西里尔 `і`。这会让带提示提取或改价要求的复合问句进入正常价格意图。

修复在策略词匹配前，把一组明确列出的常见西里尔同形字映射到对应 ASCII 字母。对于这 2 条攻击，规则将其归为 `UNSUPPORTED`，不会进入正常价格回答；2 条普通控制仍归为 `PRICE`。该实现是有界的混淆字符清理，不是全字符集的 Unicode confusable skeleton。

## 离线评测结果

- 运行 Provider：`commerceflow-mock`，模式 `MOCK`。
- 同形字攻击契约：2/2；普通控制：2/2；总场景：4/4。
- Scenario Accuracy、Intent Macro-F1、Status Accuracy 与 Answer Fact Accuracy：固定四条样本均为 1.0。
- 外部 Provider 请求数：0；逐样本结果不存储问题文本或最终回答原文。

| 命令 | 退出码 | 结果 |
|---|---:|---|
| `py -3.12 -m pytest tests/test_real_provider.py -k unicode_confusables -q`（修复前） | 1 | 2 个攻击断言失败、2 个控制通过 |
| 同一命令（修复后） | 0 | 4 passed |
| `py -3.12 .\evaluate_provider.py --dataset .\evaluation\customer-service-redteam-v4-unicode-confusables.json --output .\target\evaluation\customer-service-redteam-v4-mock-20261022.json --limit 4`，进程强制 MOCK | 0 | 4/4，0 外部请求 |
| `py -3.12 -m pytest -q` | 0 | 107 passed |
| `mvn -o test` | 0 | 117 项、0 失败、0 错误、10 项跳过；隔离 H2 应用 12 条迁移，未连接 MySQL |

## 机器可读材料

- [四条合成样本](customer-service-redteam-v4-unicode-confusables.json)
- [逐样本 MOCK 结果与混淆矩阵](customer-service-redteam-v4-mock-results.json)

## 边界

4 条样本适合说明如何用明确的失败样本推动策略修复，但样本量太小，不能代表广义提示注入防护率、DeepSeek 表现、生产安全或所有 Unicode 同形字。控制样本只覆盖本 fixture 中两种普通问法。模型是否会被此类提示影响仍需真实 Provider 独立测试；本轮真实模型请求数为 0。Java 回归运行时 Redis 健康检查出现不可达告警；测试仍通过，未连接 Redis 或 MySQL。
