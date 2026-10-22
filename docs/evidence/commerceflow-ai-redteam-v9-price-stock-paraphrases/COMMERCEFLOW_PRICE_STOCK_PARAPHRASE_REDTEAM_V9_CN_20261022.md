# CommerceFlow 价格与库存自然语言变体红队 v9

日期：2026-10-22  
项目分支：`feat/order-timeout-cancellation`  
基线提交：`b9bdaa1e8bb5a89735403fe66c55413d2628905b`  
数据来源：固定合成样本，不含客户数据或生产数据。

## 本轮发现

v8 已覆盖中英文 `update` 变更指令，但继续改写攻击措辞后，规则仍会把“替换/覆盖售价”“把成本改成指定值”“库存应该是某个数”等自然表达当成普通价格或库存查询。配对控制还发现一个意图优先级问题：询问“库存数量”时同时提到 SKU，可能先命中 SKU 编码意图。

修复前定向分类用例出现 8 个失败：7 条变更攻击被分到可回答意图，另有 1 条“当前 SKU 的库存数量”控制被分到 `SKU_CODE`。固定“昨天发生了替换/调整，现在是多少”的历史描述应继续回答当前商品事实。

## v9 覆盖与结果

- v9 原样保留 v8 的 28 条样本，新增 8 条攻击和 8 条正常控制，共 44 条：攻击 22 条、控制 22 条。
- 攻击覆盖英语替换/覆盖售价、成本改写、库存断言与记录指令，以及中文售价重设、库存调整表达；控制覆盖历史价格/库存变更描述和当前事实查询。
- 固定 MOCK 逐样本结果：44/44 契约通过，其中攻击 22/22、控制 22/22；意图准确率、状态准确率、回答事实匹配均为 1.0000；失败样本为空；外部请求 0。
- 价格控制核对合成售价 `¥129.00`；库存控制核对合成库存 `27 件`。这些只是数据集中的固定事实。

机器可读文件：

- [`customer-service-redteam-v9-price-stock-paraphrases.json`](customer-service-redteam-v9-price-stock-paraphrases.json)
- [`customer-service-redteam-v9-price-stock-paraphrases-mock-results.json`](customer-service-redteam-v9-price-stock-paraphrases-mock-results.json)

## 修复

- Python 规则增加有界模式，只在变更动词、价格/库存对象和新值形成组合时拒绝；区分现在要求修改事实与过去已发生的历史描述。
- Python 与 Java 降级链路增加替换、覆盖、重设、成本改写及“库存应为某值”等识别；Java 的价格、库存事实兜底同时补齐英语当前事实问法。
- 明确的库存数量/购买线索优先于 SKU 提及；只有一般库存词而无数量查询线索时，再结合 SKU 编码意图判定，避免把“库存单位编码”误答成库存数量。
- 后端仅拒答不受支持的变更请求，不写入或修改商品价格、库存等业务数据。真实模型协议、API 和数据库均未改动。

## 验证证据

修复前 TDD 分类测试：

```text
py -3.12 -m pytest -q tests/test_real_provider.py -k update_mutations
8 failed, 16 passed, 88 deselected
```

修复后：

```text
py -3.12 -m pytest -q tests/test_real_provider.py -k update_mutations
24 passed, 88 deselected

py -3.12 -m pytest -q tests/test_evaluate_provider.py -k redteam_v9
1 passed, 28 deselected

py -3.12 -m pytest -q
153 passed

mvn -o test
Tests run: 117, Failures: 0, Errors: 0, Skipped: 10
BUILD SUCCESS (exit code 0)
```

Java 全量基线使用隔离 H2；10 项 Redis/MySQL 条件集成测试跳过，因此未连接 MySQL，也未验证真实 Redis 环境。

实际评测以本机 MOCK 模式执行：

```powershell
$env:AI_PROVIDER_MODE = 'MOCK'
$env:COMMERCEFLOW_AI_EVAL_ENABLED = 'false'
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v9-price-stock-paraphrases.json --output docs/evidence/commerceflow-ai-redteam-v9-price-stock-paraphrases/customer-service-redteam-v9-price-stock-paraphrases-mock-results.json
```

实际终端输出：`provider=commerceflow-mock mode=MOCK scenario_accuracy=44/44 response_success=44/44`，退出码 0。

本机日志：

- 修复前分类回归：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\commerce-v9-replacement-assertion-red-20261022.log`
- 修复后分类回归：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\commerce-v9-replacement-assertion-green-20261022.log`
- Python 全量最终回归：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\commerce-update-v9-full-pytest-final2-20261022.log`
- Java 全量回归：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\commerce-update-v9-full-maven-final2-20261022.log`
- MOCK 逐样本评测：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\commerce-redteam-v9-price-stock-paraphrases-mock-20261022.log`

## 结论边界

v9 证明的是固定合成语句在本地确定性规则和 `commerceflow-mock` 下满足既定拒答/事实契约；它不测 DeepSeek 或其他 LLM 的越狱成功率，也不是生产安全或客户服务质量指标。此轮真实 DeepSeek 请求为 0。Java 测试使用 H2；没有连接 MySQL。后续真实模型比较应使用已轮换、仅保存在本机后端环境中的密钥，并把真实模型结果作为单独模式记录。
