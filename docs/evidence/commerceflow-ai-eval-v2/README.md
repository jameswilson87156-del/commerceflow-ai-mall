# CommerceFlow AI 客服评测 v2

## 本轮范围与结果

- 数据：22 条合成客服问题，覆盖 PRICE、SPECIFICATION、SKU_CODE、STOCK_OR_PURCHASE、UNSUPPORTED 五类；包含两条同时问价格和库存的复合样本。历史 v1 的 20 条数据保留不变。
- Mock 离线：22/22 场景通过，意图 Accuracy/Macro-F1、状态正确率、事实正确率均为 1.0000。此为确定性 Mock 功能覆盖，不是模型成绩；外部请求为 0。
- DeepSeek Flash 真实评测：22 个请求，批次 10、10、2；没有重试，0 个 Provider/解析失败。
- DeepSeek：场景 22/22，结构化意图 Accuracy 与 Macro-P/R/F1 均为 1.0000，状态 22/22，要求事实匹配 22/22。
- 两条复合问题都识别为 STOCK_OR_PURCHASE，并在最终服务端回答中同时包含该样本要求的库存和价格事实。
- 全部数字只描述这 22 条固定合成样本，不代表真实顾客流量或生产质量。

## 意图支持数

| 意图 | 样本数 | 命中 |
| --- | ---: | ---: |
| PRICE | 4 | 4 |
| SPECIFICATION | 4 | 4 |
| SKU_CODE | 4 | 4 |
| STOCK_OR_PURCHASE | 6 | 6 |
| UNSUPPORTED | 4 | 4 |

真实模型混淆矩阵为对角矩阵，无 UNCLASSIFIED 或 Provider 失败样本。逐样本类别与事实匹配见 commerce-v2-real-deepseek-summary.json。

## 事实边界

Java 商城服务根据商品与 SKU 记录组装 businessFacts，再交给 Python AI Provider。Provider 返回结构化意图/claims 后，Java 校验状态、意图和 claims；最终给用户的回答由服务端按业务事实渲染，避免把模型自由生成内容当作价格或库存事实。本评测检查样本意图、回答状态和答案是否包含每条样本要求的事实，不代表对所有幻觉形式的完整安全证明。

## 回归与可复现信息

- 项目分支 feat/order-timeout-cancellation，基线 HEAD b9bdaa1e8bb5a89735403fe66c55413d2628905b；工作区保留未提交实现与测试改动。
- 工作目录 services/ai-service。2026-10-21 执行 py -3 -m pytest -q，退出码 0，79 passed；完整日志在外层交付目录 commerce-regression-final-v3.log。
- 新离线报告 customer-service-eval-v2-mock.json：22 条、MOCK、外部请求 0；场景、意图、状态和事实指标均为 1.0。
- 本轮真实模型请求最多生成 2048 tokens/条，连接/读取超时 2/10 秒，不重试；合计 prompt 11,568、completion 7,756、total 19,324 tokens。
- 机器可读 JSON 已移除请求 ID、密钥和模型原始响应。customer-service-cases-v2.json 是新增合成数据；历史 v1 文件保留。
- 真实请求批次及全量回归的原始日志保留在外层交付目录供核验，不含 API Key。
