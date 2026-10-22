<!-- Public copy: local paths and credential-environment details removed. Synthetic evaluation data only. -->

# CommerceFlow AI Mall｜DeepSeek 双模型评测报告

日期：2026-10-22  
结论：**COMPLETE_WITH_FINDINGS**（主评测、定向修正和离线回归完成；失败样本保留）

## 项目与配置

- 仓库：本地隔离工作区 <local workspace>
- 分支：feat/order-timeout-cancellation
- HEAD：b9bdaa1e8bb5a89735403fe66c55413d2628905b
- Provider：DeepSeek OpenAI-compatible API；模型 deepseek-flash、deepseek-v4-pro；地址 https://api.deepseek.com
- 全部评测输入为合成商品、SKU、库存、复合咨询与红队文本，不含用户或生产数据。
- 这里的“场景通过数”包括真实模型调用和本地策略门禁；必须结合列出的请求数解读，不能写成模型准确率。

## 主评测结果

每个模型共跑 158 个场景：商品价格/库存等核心场景 44、复合意图 18、受支持注入集 6、红队与正常控制 90。

| 数据集 | 场景数 | Flash 通过 | Pro 通过 |
| --- | ---: | ---: | ---: |
| 核心价格/库存 v3 | 44 | 44 | 43 |
| 复合意图 v2 | 18 | 17 | 18 |
| 受支持注入 | 6 | 6 | 6 |
| 红队/正常控制 v13 | 90 | 90 | 90 |
| **合计场景断言** | **158** | **157** | **157** |

两模型都各有 105 次真实 Provider 请求尝试和 53 个本地策略门禁场景。真正进入并成功形成可评测模型结果的行数为 104/105。失败分别是：

- Flash：CIM18，PROVIDER_FACT_MISMATCH。用户用“还剩 N 件 / 有货”询问库存并同时问价格时，意图分流选择了不合适的答案事实组合；服务端校验拒绝不一致结果，没有把错误事实作为成功回答返回。
- Pro：CS12，REMOTE_PROVIDER_TIMEOUT。主评测使用 10 秒评测超时，按失败计入主结果；单独 30 秒诊断跟进 1/1 成功（约 6.95 秒），没有自动重试，且未改写主评测成绩。

**157/158 是混合场景断言结果，不是 LLM 准确率。** 其中 53 个场景由本地策略直接拦截/处理，未调用模型。

## 红队与正常控制

红队 v13 每个模型共 90 条：45 条攻击场景、45 条正常控制。每个模型各发出 45 次模型请求，其余 45 条由本地策略门禁完成。

- 攻击合约：45/45。
- 正常控制合约：45/45。
- 外层状态与事实断言：90/90。
- 该结果只说明固定合成输入在当前检测/路由契约下的行为，不是通用安全性或生产防护保证。

## 定向修复

在 FastAPI Provider 提示中明确识别 “N units left”“N items remaining”“in stock” 为库存可用性问题；复合问题同时问价格时，优先走库存/购买主意图，再由服务端基于 Java 提供的业务事实渲染所需事实。

- 修改文件：services/ai-service/app/providers.py
- 对应测试：services/ai-service/tests/test_real_provider.py
- 最终 CIM18 定向跟进：Flash 1/1、Pro 1/1。此结果是提示词修正后的单条诊断，**没有覆盖或重算**主评测的 Flash 17/18。
- 没有自动重试、没有改写原始标签或主报告。

## 离线回归

| 状态 | 命令 | 工作目录 | 退出码与结果 | 日志 |
| --- | --- | --- | --- | --- |
| NOT_RUN（启动失败） | python -m pytest -q | <local workspace> | 9009；Windows Store Python 别名未找到解释器 | 原始 pytest 终端日志保留在本机交付包，未公开包含环境路径的日志。 |
| PASS | Python 3.12 -m pytest -q | 同上 | 0；224 项通过 | 原始 pytest 终端日志保留在本机交付包，未公开包含环境路径的日志。 |
| PASS | 定向价格/库存提示边界测试 | 同上 | 0；1 项通过 | 原始 pytest 终端日志保留在本机交付包，未公开包含环境路径的日志。 |

最后一次全量回归运行没有使用 API Key，也未发送真实模型请求。

## 请求、Token 与估算费用

双项目所有主批次、红队批次及定向补测合计：

- Flash：267 次请求尝试；125,483 输入 Token、76,144 输出 Token 有 usage 记录；1 次请求未返回 token usage。
- Pro：269 次请求尝试；139,724 输入 Token、137,828 输出 Token 有 usage 记录；2 次请求未返回 token usage。
- 总计：536 次请求尝试；509 条 usage 记录；265,207 输入 Token、213,972 输出 Token；3 次请求没有返回 token usage。
- 按 DeepSeek 页面列出的峰时 cache-miss 价格，仅对有 usage 的请求估算约 **US$0.859253**。这不是账单：缓存命中、峰谷价格和没有 usage 的请求均可能改变实际扣费。[DeepSeek 官方价格表](https://api-docs.deepseek.com/quick_start/pricing/)

## 证据文件

- [结构化逐样本与批次结果](RAW_RESULTS.jsonl)：包含来源相对路径、原始文件 SHA-256 和解析后的 JSON/JSONL 记录；公开副本中的绝对 Windows 路径已替换为 `<REDACTED_LOCAL_PATH>`。
- [双项目统一汇总](AGGREGATED_RESULTS.json)。
- [源文件清单与 SHA-256](RESULTS_MANIFEST.json)；SHA-256 对应本机原始文件。


## 工作区与密钥

本地工作区在开始时已有未提交业务修改与未跟踪评测资料。本轮未重置、清理、提交、推送或部署；只改动 AI Provider 提示和对应测试文件，新增证据均写到独立交付目录。

当前执行环境中 DEEPSEEK_API_KEY 未配置。没有 Key 值写入源码、评测 JSON 或报告。该 Key 曾出现在聊天中，历史一次 CLI 进程参数也曾暴露过；请立即撤销并轮换，再由本人将新 Key 配置到本机私密环境变量。
