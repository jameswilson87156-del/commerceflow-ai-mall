# CommerceFlow Unicode 混淆红队 V10：当前代码复跑记录

日期：2026-10-22  
运行模式：`MOCK`  
目的：在最近复合问句实现之后，重新运行同一固定数据集，确认旧报告仍适用于当前代码。

## 结果

|检查项|结果|
|---|---:|
|合成样本|54/54|
|攻击用例|27/27 契约通过|
|正常控制|27/27 契约通过|
|意图、状态、答案事实断言|54/54|
|失败样本|0|
|外部 Provider 请求|0|
|与之前 MOCK 报告的行为字段差异|0|

对照脚本逐条核对样本 ID、意图、状态、意图/状态/答案断言、禁用标记和策略契约；延迟时间与生成时间不用于行为对比。结果 JSON 保留结构化逐样本字段，不保留原始回答。

## 复现

工作目录：`services/ai-service/`

```powershell
$env:AI_PROVIDER_MODE = "MOCK"
$env:COMMERCEFLOW_AI_EVAL_ENABLED = "false"
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v10-unicode-confusables.json --output docs/evidence/commerceflow-ai-redteam-v10-unicode-confusables/customer-service-redteam-v10-current-mock-results-20261022.json
py -3.12 evaluation/validate_redteam_v10_current_mock.py
```

评测与验证命令退出码均为 0。逐样本结果：`customer-service-redteam-v10-current-mock-results-20261022.json`；日志：`commerce-redteam-v10-current-mock-eval-20261022.log`、`commerce-redteam-v10-current-mock-validation-20261022.log`。

## 解释边界

此次复跑证明当前本地 `commerceflow-mock` 路由仍满足这 54 条固定合成数据的断言，且与此前 MOCK 行为一致。它不检验真实大模型、自由文本生成质量、线上用户数据或未知攻击，也不构成通用攻击成功率。真实模型评测仍为 `NOT_RUN`。
