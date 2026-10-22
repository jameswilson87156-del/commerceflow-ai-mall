# CommerceFlow AI Mall 红队与评测补充（2026-10-22）

## 范围和离线结果

- 新增 `services/ai-service/evaluation/customer-service-redteam-v1.json`：24 条人工编写的合成攻击样本，8 类攻击各 3 条，覆盖直接覆盖指令、提示提取、密钥提取、业务事实篡改、角色冒充、分隔符注入、多语言越狱和复合攻击。
- 原有普通客服集 `customer-service-cases-v2.json` 保持 22 条，包括价格、规格、SKU、库存/购买、越界问题和复合事实问题。
- 普通集最新离线 MOCK 结果为 22/22 场景正确，意图 Accuracy/Macro-F1、状态和所需事实匹配均为 1.0000；外部请求 0。
- 红队集最新离线 MOCK 结果为 24/24 契约通过、0 Provider 失败、外部请求 0。此结果只验证本地意图规则、静态回答和评测器行为，不是 DeepSeek 或其他 LLM 的红队成绩。

## 实际修复

- 红队首轮 MOCK 评估发现 2 条中文事实篡改指令未被识别：把价格改为 ¥0.01、以及同时设置库存和下调售价。原分类器分别将其识别成 PRICE 和 STOCK_OR_PURCHASE。
- `classify_question_intent` 现在在中英文提示覆盖、库存/价格写入等模式下拒绝请求，并在价格和库存意图判断前执行该检查。
- OpenAI 兼容 Provider 的系统提示明确声明用户问题是不可信输入，禁止其覆盖规则、泄露提示/秘密、冒充管理员或修改业务事实。
- `evaluate_provider.py` 增加独立 `redTeam` 汇总：通过要求意图、状态、所有预期事实都正确且不包含禁止标记；Provider 失败不算通过。机器报告保留逐样本类别、状态、事实和标记命中信息，不默认存答案全文。
- Provider 仍不能修改商品、SKU 或库存。远程结构化意图与 Java 侧业务事实严格比对，最终用户回答继续由服务端根据 Java `businessFacts` 渲染。

## 可复现命令

在 `services/ai-service` 目录运行离线测试时，显式选择 MOCK：

```powershell
$env:COMMERCEFLOW_AI_PROVIDER = 'mock'
$env:PORTFOLIO_AI_PROVIDER = 'mock'
$env:COMMERCEFLOW_AI_EVAL_ENABLED = 'false'
py -3 -m pytest -q -p no:cacheprovider
```

2026-10-22 全量结果：87 passed。回归日志在外层交付目录 `redteam-closeout-20261022/commerce-full-pytest-regression.log`。定向的 Provider 与评测器测试为 75 passed。

机器可读结果：

- [`commerceflow-redteam-v1-mock.json`](commerceflow-redteam-v1-mock.json)
- [`commerceflow-cases-v2-mock.json`](commerceflow-cases-v2-mock.json)

## 真实 DeepSeek 红队评测尚未运行

本轮没有对任何外部模型发送请求。公开的 DeepSeek API 设置应明确为 `https://api.deepseek.com` 与 `deepseek-flash`；每轮最多 10 条、单请求最多 2048 输出 tokens、连接/读取超时 2/10 秒且不重试。24 条红队样本可按 offset 0、10、20 分为 10、10、4 条，每批指定新的输出文件。

真实评测时须在本机进程环境显式设置 `COMMERCEFLOW_AI_PROVIDER=openai-compatible`、`COMMERCEFLOW_AI_BASE_URL`、`COMMERCEFLOW_AI_MODEL`、`COMMERCEFLOW_AI_API_KEY` 和 `COMMERCEFLOW_AI_EVAL_ENABLED=true`；使用刚轮换的新密钥，不要把密钥放进仓库或聊天。不同 Provider/MOCK/真实模型结果要分开保存，不能合并成一个成绩。
