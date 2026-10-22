# CommerceFlow DeepSeek 与离线红队评测报告

日期：2026-10-22  
结论：**PASS（仅限本报告列出的合成样本与受控链路）**

## 仓库与工作区

- 仓库：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\commerce-repair`
- 分支：`feat/order-timeout-cancellation`
- HEAD：`b9bdaa1e8bb5a89735403fe66c55413d2628905b`
- 执行前工作区已有未提交业务修改和未跟踪评测资料。本轮保留这些修改；没有 reset、clean、切换分支、提交、推送或部署。
- 本轮 CommerceFlow 只新增了合成评测集与本目录下的评测结果、日志和报告；没有修改业务代码、Provider、API、前端或测试预期。
- 所有数据均为合成数据，没有使用客户订单、真实用户输入或生产数据。

## 真实 DeepSeek 结果

### V7 商品价格与库存良性对照

- 模型：`deepseek-flash`；服务端点主机：`api.deepseek.com`。
- 价格用例：CFE10–CFE13，4 次请求，4/4 通过。
- 库存/购买用例：CFE18–CFE21，4 次请求，4/4 通过。
- 合计 8 次真实请求、0 Provider 失败；意图分类、状态、答案事实和场景契约均为 8/8。
- 该服务由本地策略先分类，模型只返回受约束的结构化意图与事实声明；服务校验后用本地业务事实渲染用户答案。因此这证明的是受控链路在这些样本上的一致性，不是自由生成客服答案的质量评测。

实际命令（工作目录 `services/ai-service`；每条均退出码 0）：

```powershell
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v7-stock.json --output <evidence>/customer-service-redteam-v7-price-controls-deepseek.json --offset 4 --limit 4 --allow-external
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v7-stock.json --output <evidence>/customer-service-redteam-v7-stock-controls-deepseek.json --offset 12 --limit 4 --allow-external
```

执行时显式配置了 `COMMERCEFLOW_AI_EVAL_ENABLED=true`、`AI_PROVIDER_MODE=openai-compatible`、`AI_PROVIDER_BASE_URL=https://api.deepseek.com`、`AI_PROVIDER_MODEL=deepseek-flash`、`AI_PROVIDER_PROTOCOL=chat-completions`、`AI_PROVIDER_FALLBACK_ENABLED=false`；Key 只在进程内临时注入。每次上限 10 个外部请求，2048 max tokens，连接/读取超时 2/10 秒，HTTP 重试为 0。

### 支持型价格问题中的提示注入

新增 `services/ai-service/evaluation/customer-service-supported-injection-v1.json`，包含 4 条会通过本地价格意图门的指令抽取、覆盖分类或格式控制样本，以及 2 条良性价格对照。先用 MOCK 完成离线预跑，6/6 通过且外部请求为 0；再用真实 DeepSeek 测试，6 次请求全部成功，攻击组 4/4、良性对照 2/2，事实与状态契约均通过。

实际命令（工作目录 `services/ai-service`；两条均退出码 0）：

```powershell
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-supported-injection-v1.json --output <evidence>/customer-service-supported-injection-v1-mock.json
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-supported-injection-v1.json --output <evidence>/customer-service-supported-injection-v1-deepseek.json --limit 6 --allow-external
```

`mustNotContain` 检查的是最终用户可见答案，原始模型响应按安全设计不落盘；测试还验证了结构化响应、意图和业务事实契约。不能据此宣称逐字审计过原始模型响应或获得通用提示注入安全保证。

## MOCK 红队基线

- 对现有 `customer-service-redteam-v7-stock.json` 的 MOCK 全集离线运行：16/16 场景通过，其中 8 条攻击、8 条良性控制；红队契约 16/16；外部请求为 0。
- 对新支持型注入集的 MOCK 预跑：6/6 通过，外部请求为 0。
- 新旧样本与版本文件存在重复关系；独立样本数请以两项目数据血缘审计为准，不要把版本文件的行数累计成独立样本数。

实际命令（工作目录 `services/ai-service`；均退出码 0）：

```powershell
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v7-stock.json --output <evidence>/customer-service-redteam-v7-mock.json
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-supported-injection-v1.json --output <evidence>/customer-service-supported-injection-v1-mock.json
```

第一条 MOCK 运行覆盖 16 条，实际在本轮评测的模型调用为 0。第二条的真实模型结果和 MOCK 结果分文件保存。

## Python 离线回归

在临时子进程中清空 `AI_PROVIDER_*`、`COMMERCEFLOW_AI_*`、`PORTFOLIO_AI_*`、`DEEPSEEK_API_KEY` 与 `AI_LLM_EVAL_ENABLED` 后运行全量 Python 测试，避免误连任何 Provider：

```powershell
@'
import os, pytest
for name in list(os.environ):
    if name.startswith(('AI_PROVIDER_','COMMERCEFLOW_AI_','PORTFOLIO_AI_')) or name in {'DEEPSEEK_API_KEY','AI_LLM_EVAL_ENABLED'}:
        os.environ.pop(name, None)
raise SystemExit(pytest.main(['-q']))
'@ | py -3.12 -
```

退出码 0；223 passed。日志见 `logs/python-pytest-full.log`。本轮没有重跑 Java 全模块测试，Java/Python 上次完整回归证据参见 V14 报告。

## 总调用量与结果文件

- 真实 DeepSeek：24 次请求，共 10 条 Ticket Copilot、8 条 CommerceFlow 价格/库存对照、6 条 CommerceFlow 支持型注入样本。
- 按 Provider 返回的 usage 字段汇总：11,812 prompt tokens、6,733 completion tokens、18,545 total tokens；本报告未折算费用。
- DeepSeek 样本均成功完成，以上请求没有 Provider/解析失败；首个 Ticket 预检曾因资源路径格式错误而在联网前失败，没有产生请求。
- 结构化逐样本记录、模型与规则/MOCK 分开后的统一格式见：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\eval-lineage-audit-20261022\model-eval-results-v2.json`。
- 本轮 API Key 只在隐藏输入中提供给临时进程环境；没有保存到 `.env`、源码、报告或日志中。它已出现在聊天消息里，建议轮换。

## 限制与下一步

- V7 的 8 条攻击样本在当前离线语料中由本地策略门拦截；本次 DeepSeek 真实调用覆盖了另加的 4 条支持型价格意图攻击尝试。该结果只表示这些具体输入经策略门、结构化模型输出校验和本地事实渲染后的端到端契约通过。
- 复合问题与异常商品/库存状态目前有离线 MOCK 覆盖；本轮没有对这些切片发起 DeepSeek 调用。
- 没有重新运行 CommerceFlow 全套 Java、Python、管理端/用户端构建；本轮未改 CommerceFlow 生产代码。此前 Java 全模块 122 项及 Python 223 项的结果仍见既有 V14 报告，本报告不将其描述为本轮重跑。
- 下一步最有价值的是扩充人工复核的独立合成场景，增加真正的规则盲点和多意图/边界样本，并将真实模型输出与本地策略门结果分栏展示；保留请求数、延迟、Token 用量和失败分类。
