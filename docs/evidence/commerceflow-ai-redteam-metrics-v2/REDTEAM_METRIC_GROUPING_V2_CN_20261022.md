# CommerceFlow 红队报告攻击/控制分组口径修订 V2

## 修订原因

旧版 `_red_team_metrics` 把所有带 `attackType` 的样本都放进 `byAttackType`。v4/v5 中正常价格控制也带 `attackType`，所以按攻击类型的汇总会把控制混入攻击统计。当前评测器读取 `caseGroup`（`attack`/`control`），分别给出攻击契约通过率与控制通过率，并只在 `byAttackType` 中统计攻击样本；未标组的历史样本保持向后兼容，默认为攻击组。

同时补充了逐样本 `caseGroup`，没有更改既有标签、答案事实或攻击期望。v4/v5 的新结果为重跑产物，旧 JSON 保留在原证据目录中未覆盖。

## MOCK 重算结果

| 数据集 | 总样本 | 攻击通过 | 正常控制通过 | 失败 | 外部请求 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Unicode 同形字 V4 | 4 | 2/2 | 2/2 | 0 | 0 |
| 百分号/HTML 实体 V5 | 4 | 2/2 | 2/2 | 0 | 0 |
| 显式编码 V6 | 8 | 4/4 | 4/4 | 0 | 0 |

附件保留各版输入夹具和重算后的逐样本 JSON。所有结果均由 `commerceflow-mock` 生成；这些小型固定集合只能作为回归证据，不可解释为真实模型或生产流量准确率。

## 验证

| 命令 | 结果 |
| --- | --- |
| `py -3.12 -m pytest -q tests/test_evaluate_provider.py -k 'separates_attack_results or focused_redteam_reports'`（目录：`services/ai-service`） | 4 项通过，0 失败；验证攻击/控制分组及 v4/v5/v6 指标 |
| `py -3.12 -m pytest -q`（目录：`services/ai-service`） | 125 项通过 |

重算命令及逐次输出日志：[V4](../../../../../deliverables/customer-service-redteam-v4-mock-regrouped-20261022.json.log) · [V5](../../../../../deliverables/customer-service-redteam-v5-mock-regrouped-20261022.json.log) · [分组回归测试](../../../../../deliverables/commerce-redteam-control-metrics-targeted-v2-20261022.log)。
