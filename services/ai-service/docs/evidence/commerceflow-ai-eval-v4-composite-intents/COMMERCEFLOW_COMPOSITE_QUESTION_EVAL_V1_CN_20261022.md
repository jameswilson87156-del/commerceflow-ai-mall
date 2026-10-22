# CommerceFlow AI Mall：价格与库存复合问句离线评测 V1

日期：2026-10-22  
评估方式：本地 `MOCK` Provider，未调用外部模型  
结论：**PASS（仅限固定合成场景断言）**

## 评测目的

旧评测集没有单句同时询问价格和库存的场景。本版新增 12 条中英文合成问句，要求回答同时包含价格事实和当前购买状态，覆盖有货、库存为零和商品下架三种状态。期望分类仍使用系统既有单标签 `STOCK_OR_PURCHASE`，没有改写旧数据集标签；复合问题通过“答案是否覆盖所有必需事实”单独衡量。

样本构成为：有货 5 条、零库存 4 条、下架 3 条。每条结果都保留在 [`customer-service-composite-v1-mock-results-20261022.json`](customer-service-composite-v1-mock-results-20261022.json) 中，包含预期事实、匹配情况、最终回答与 Provider 模式。输入数据见项目评测目录 [`customer-service-composite-intents-v1.json`](../../../evaluation/customer-service-composite-intents-v1.json)。

## 结果

|检查项|结果|
|---|---:|
|场景断言（状态与答案事实）|12/12|
|回答状态断言|12/12|
|答案必需事实断言|12/12|
|既有主意图标签匹配（MOCK）|12/12|
|外部请求|0|

下架或零库存时，如果问题也单独询问价格，回答会保留价格并说明无法购买；只有“how much is available”一类库存数量问法不会被当成价格问题追加价格。正常有货的库存回答沿用原有模板。

## 复现与测试

工作目录：`services/ai-service/`

```powershell
$env:AI_PROVIDER_MODE = "MOCK"
$env:COMMERCEFLOW_AI_EVAL_ENABLED = "false"
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-composite-intents-v1.json --output docs/evidence/commerceflow-ai-eval-v4-composite-intents/customer-service-composite-v1-mock-results-20261022.json --include-answer-text
py -3.12 -m pytest -q
```

- 评测命令退出码 0；独立 JSON 校验退出码 0；没有真实模型请求。
- 完整 Python 回归：177 项通过；退出码 0。
- 定向复合场景测试覆盖正常库存、零库存、下架商品和“只问库存数量”的边界。
- 完整日志位于本轮交付目录：`commerce-composite-v1-mock-eval-final-20261022.log`、`commerce-composite-v1-json-validation-final-20261022.log`、`commerce-full-pytest-final-20261022.log`。

## 结果解释与限制

这些是本地 MOCK 与固定 Java 业务事实下的答案模板/路由断言，不是 DeepSeek 的意图识别、自然语言理解或回答质量结果，也不代表线上数据表现。单标签意图指标只检查既有主意图；复合能力通过多事实答案断言衡量。短语切分和同义词规则仍是有限启发式，不能覆盖任意表达。
