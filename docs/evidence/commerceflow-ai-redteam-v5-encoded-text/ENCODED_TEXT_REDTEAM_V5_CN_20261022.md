# CommerceFlow 编码文本输入红队补测（2026-10-22）

## 目标与范围

根据公开红队项目将输入策略单独建组的做法，新增 v5 编码文本合成夹具。本次覆盖百分号编码与数字 HTML 实体的指令覆盖文本，每类配价格问题控制样本，共 4 条。结果来自本地 commerceflow-mock，没有调用 DeepSeek，也不代表真实模型的越狱防护能力。

## 红测、修复与复测

- 初始红测：2 条攻击均绕过分类检查、落入 PRICE；2 条价格控制正确。
- 修复后：scenarioAccuracy、responseSuccessRate、意图 accuracy/Macro-F1、状态正确率和事实正确率在这 4 条固定样本均为 1.0；契约通过 4/4，外部请求尝试 0。
- 逐样本结果位于 customer-service-redteam-v5-mock-results.json；原始合成夹具位于 customer-service-redteam-v5-encoded-text.json。
- 夹具校正记录：第一次试跑中一条攻击意外命中既有“访问令牌”词面策略；为隔离编码解码行为，将样本改为与另一攻击同类型的编码指令。修正后红测明确复现两种编码攻击，避免把既有词面规则误当作解码覆盖。

## 实现与限制

分类器在处理文本前最多执行两轮 HTML 实体与百分号解码，再做既有 NFKC、格式字符清理及有限西里尔同形字映射。当前仅支持已列出的编码形式与合成样本；没有验证任意编码、复杂嵌套、语义改写或真实模型行为。

## 验证

- 定向 Python：py -3.12 -m pytest -q tests/test_real_provider.py -k encoded_instructions，4 passed，退出码 0。
- MOCK 评测：py -3.12 .\evaluate_provider.py --dataset .\evaluation\customer-service-redteam-v5-encoded-text.json --output .\target\evaluation\customer-service-redteam-v5-mock-20261022.json --limit 4，4/4，外部请求 0，退出码 0。
- Python 全量：py -3.12 -m pytest -q，111 passed，退出码 0。
- Java 全量：mvn -o test，117 项、0 失败、0 错误、10 项跳过，退出码 0；Redis 与 MySQL 集成测试各跳过 5 项，本机没有配置这两项隔离服务，常规测试在 H2 上运行。
- 本轮真实 DeepSeek 请求：0。逐样本结果与历史真实模型结果分开保存。

## 面试讲法

“我用两种常见文本编码做了一个独立小型红队集，并加了相同业务的价格控制。第一次复测发现两类编码指令都被分类成价格问题，我在本地预处理链增加了有界 HTML/百分号解码，再执行原有归一化和策略分类。修复后 4 条 MOCK 契约样本通过，Python 与 Java 全量回归也通过；这个结果只说明这组离线样本覆盖，不是 DeepSeek 安全率。”

## 公开方法参考

- NVIDIA garak 编码输入探针：https://github.com/NVIDIA/garak/blob/main/garak/probes/encoding.py
- Promptfoo 红队插件与策略配置：https://github.com/promptfoo/promptfoo/blob/main/site/docs/red-team/configuration.md
