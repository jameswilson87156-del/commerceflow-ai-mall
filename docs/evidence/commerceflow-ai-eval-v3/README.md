# CommerceFlow AI 客服评测 v3（2026-10-22）

## 本轮新增

- 保留 v2 的 22 条样本及原标签，在 `services/ai-service/evaluation/customer-service-cases-v3.json` 追加 22 条双语覆盖与回归样本，总计 44 条。
- 新样本补充历史价格/库存调整后的当前事实查询、多种表达方式、混合事实问题、当前库存问法、退货/配送/优惠/支付越界问题。
- 固定商品事实均为合成数据。44 条样本只验证当前意图与最终事实回答契约，不代表真实客户问题分布。
- 新增回归捕获了两类误拦截：用户提到历史价格调整或库存调整、但实际询问当前价格/库存时，宽泛变更词匹配曾将其误判为 `UNSUPPORTED`。规则已收窄到明确修改指令或带目标值的修改表达。

## 离线结果

实际运行命令：

```powershell
cd services/ai-service
py -3 evaluate_provider.py --dataset evaluation/customer-service-cases-v3.json --output docs/evidence/commerceflow-ai-eval-v3/customer-service-eval-mock-20261022.json --include-answer-text
py -3 evaluate_provider.py --dataset evaluation/customer-service-redteam-v1.json --output docs/evidence/commerceflow-ai-redteam-v1/metrics-mock-afterfix-with-text-20261022.json --include-answer-text
py -3 -m pytest -q
```

上述两个评测输出最初按 `services/ai-service` 工作目录写入同名 `docs/evidence/` 子目录，随后使用 `Move-Item` 原样移到仓库根目录的 `docs/evidence/`；文件内容未修改，目标文件此前不存在。

结果：

- 普通 v3：`commerceflow-mock` / `MOCK`，场景 44/44，响应 44/44，意图 Accuracy 1.0000、Macro-F1 1.0000、状态正确率 1.0000、事实正确率 1.0000；外部请求 0。
- 44 条中各意图支持数：PRICE 10、SPECIFICATION 8、SKU_CODE 8、STOCK_OR_PURCHASE 10、UNSUPPORTED 8。逐样本 ID、预测、回答与匹配事实保存在机器可读报告中；失败样本为空。
- 红队 v1：本地 MOCK 契约检查 24/24，覆盖 8 类、每类 3 条；外部请求 0。它检查本地确定性拒绝/事实契约，不是 DeepSeek 越狱成功率或模型安全率。
- 红队 v2：在 v1 基础上新增 8 条攻击前缀样本，共 32 条、8 类各 4 条；本地 MOCK 契约检查 32/32，外部请求 0。详见[红队 v2 报告](../commerceflow-ai-redteam-v2/RED_TEAM_EVALUATION_V2_CN_20261022.md)。
- 最新全量 Python 测试：94 passed，退出码 0。
- TDD 证据：新增 4 个当前事实回归用例在修复前 4 个均失败；修复后分类相关测试 25 passed，38 deselected。

## 与历史 DeepSeek 结果的区别

历史 v2 有 22 条真实 DeepSeek 请求，结构化意图、回答状态、要求事实匹配均为 22/22，共 19,324 tokens；原始汇总见 [v2 历史报告](../commerceflow-ai-eval-v2/README.md)。它覆盖的是旧 v2 样本，不能当作本轮 44 条 v3 的模型结果。

本轮运行前仅检查环境变量是否存在，不读取或输出值。CommerceFlow Python Provider 的 `AI_PROVIDER_API_KEY`、模式/URL/模型配置和显式评测开关，以及 `DEEPSEEK_API_KEY` / `AI_DEEPSEEK_API_KEY` / `COMMERCEFLOW_AI_API_KEY` 均未在进程、用户或系统环境中配置。因此 v3 和本轮红队没有真实模型请求。

## 机器可读证据

- [44 条 MOCK 逐样本结果](customer-service-eval-mock-20261022.json)
- [红队 MOCK 逐样本结果](../commerceflow-ai-redteam-v1/metrics-mock-afterfix-with-text-20261022.json)
- [44 条合成测试集](../../../services/ai-service/evaluation/customer-service-cases-v3.json)

DeepSeek 外部评测器每次最多发送 10 个合成请求，真实调用还要求显式打开环境标志和 `--allow-external`。当前 v3 计划分成五批，最后一批含 4 条。请在有新的、只存本机的密钥后再运行；不要使用已经粘贴到聊天中的旧密钥。
