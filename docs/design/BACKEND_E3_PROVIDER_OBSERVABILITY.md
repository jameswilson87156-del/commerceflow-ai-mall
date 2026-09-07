# CommerceFlow 后端 E3：Provider 适配与可观测性

> 状态：本轮已完成代码基础与本地协议验收。日期：2026-09-03。默认 Showcase 仍使用受限 FastAPI `commerceflow-mock`；GPT/DeepSeek 兼容适配器已实现并用本地兼容 HTTP fixture 验证，但当前没有使用真实 API Key、真实供应商额度或公网部署。

## 1. 本轮目标

E3 解决的是“模型供应商可以替换，但业务事实和安全边界不能跟着供应商漂移”：

1. Java AI 应用服务依赖 `CustomerServiceProvider` 出站端口，而不是依赖某个 HTTP 客户端实现；
2. FastAPI、OpenAI-compatible、OpenAI 和 DeepSeek 通过配置选择，浏览器永远不接触 Provider Key；
3. Provider HTTP 状态、超时、坏 JSON、空响应、未知状态和过大响应映射为有限错误分类；
4. 只对超时、429、5xx 等瞬时失败做最多 3 次以内的有界重试，不重试鉴权错误、普通 4xx 或结构化校验失败；
5. Provider 指标只使用低基数 `mode/outcome` 标签，不记录问题、完整回答、Prompt、Trace ID 或密钥。

## 2. 代码边界

```text
AiService
  └─ CustomerServiceProvider (out port)
       ├─ HttpCustomerServiceProviderClient
       │    └─ FastAPI /internal/ai/customer-service/answer
       └─ OpenAiCompatibleCustomerServiceProviderClient
            └─ /v1/chat/completions
                 ├─ OpenAI
                 └─ DeepSeek / compatible gateway
```

`CustomerServiceProviderClient` 仍保留在旧包中作为兼容别名，继承新的 `ai.application.port.out.CustomerServiceProvider`，用于避免现有测试和集成代码一次性断裂。`AiService` 内部保存和调用的是新的端口类型；具体适配器由 Spring 条件装配选择。

## 3. Provider 配置

默认配置不变：

```text
COMMERCEFLOW_AI_PROVIDER=FASTAPI
AI_SERVICE_URL=http://127.0.0.1:8000
```

启用 OpenAI-compatible 协议时，只通过运行环境注入以下值：

```text
COMMERCEFLOW_AI_PROVIDER=OPENAI_COMPATIBLE
COMMERCEFLOW_AI_BASE_URL=https://api.deepseek.com
COMMERCEFLOW_AI_PATH=/v1/chat/completions
COMMERCEFLOW_AI_MODEL=deepseek-chat
COMMERCEFLOW_AI_API_KEY=<secret-manager-or-runtime-secret>
COMMERCEFLOW_AI_PROVIDER_NAME=deepseek
COMMERCEFLOW_AI_CONNECT_TIMEOUT_MS=1500
COMMERCEFLOW_AI_READ_TIMEOUT_MS=8000
COMMERCEFLOW_AI_MAX_RETRIES=1
COMMERCEFLOW_AI_FALLBACK_ENABLED=true
```

针对 OpenAI，只需把 `BASE_URL`、`MODEL` 和 `PROVIDER_NAME` 换成对应环境值；也可以把 Provider 值写成 `DEEPSEEK` 或 `OPENAI`，它们会复用同一兼容协议适配器。`FASTAPI`、`OPENAI_COMPATIBLE`、`OPENAI`、`DEEPSEEK` 是模式选择，不是供应商质量或可用性的证明。

## 4. 请求和响应安全

- Java 只发送当前问题和 Java 从 Product/SKU/Inventory 读取的 `businessFacts`；Provider 不访问 MySQL，也不接收数据库连接信息。
- API Key 只用于 `Authorization: Bearer ...` 请求头；不进入 JSON body、Trace、Outbox、异常消息或 Micrometer 标签。
- System 指令固定在服务端适配器中，前端问题作为不可信业务字段发送；适配器要求 JSON 结果，禁止工具调用和 Markdown 作为协议输出。
- 外部响应先检查 HTTP 状态，再解析 `choices[0].message.content`，支持结构化 JSON 和受限纯文本；Java `AiService` 继续校验 Trace、状态、Provider mode、答案非空和 600 字符上限。
- 结构化输出中的 `PROVIDER_ERROR`/`FALLBACK_ANSWER` 不会被当作成功模型回答；超时、不可达、坏 JSON、鉴权失败和状态不合法最终进入既有 Java fact fallback 或明确拒答。
- 适配器错误只返回固定分类：`TIMEOUT`、`UNAVAILABLE`、`INVALID_RESPONSE`、`PROVIDER_ERROR`。不会把供应商原始响应体回传给浏览器。

## 5. 可观测性

`AiProviderMetrics` 注册以下低基数指标：

| 指标 | 标签 | 语义 |
| --- | --- | --- |
| `commerceflow.ai.provider.requests` | `mode`, `outcome` | Provider 调用结果计数 |
| `commerceflow.ai.provider.latency` | `mode` | Provider 调用耗时计时器 |

`outcome` 只允许 `SUCCESS`、`PROVIDER_ERROR`、`TIMEOUT`、`UNAVAILABLE`、`INVALID_RESPONSE`。问题文本、回答文本、用户标识、Correlation/Trace ID 和密钥不作为标签；运营页的 `runtimeBoundary.aiMode` 只显示 `MOCK` 或 `REAL_OPENAI_COMPATIBLE`。

## 6. 测试证据

- `AiProviderBoundaryTests`：端口继承、应用服务只保存新端口、默认模式和别名模式。
- `OpenAiCompatibleCustomerServiceProviderTests`：兼容响应解码、鉴权头、请求体不含密钥、429 有界重试、401 不重试和坏 JSON。
- `AiExternalProviderContextTests`：Spring 外部模式只装配兼容适配器，默认 FastAPI 适配器不重复装配。
- `AiProviderObservabilityTests`：指标只产生固定 mode/outcome 维度。
- 既有 `AiCustomerServiceTests`、Redis 限流和 Operations 测试继续回归，默认 Showcase 仍返回 `commerceflow-mock / MOCK`。

本轮验证的是本地伪造的兼容 HTTP 服务和 Spring/H2 上下文，不是 OpenAI/DeepSeek 真实模型质量、计费、限额或公网链路验收。

## 7. 下一阶段边界

E3 之后仍需要 E4 staging：真实身份、密钥管理/轮换、HTTPS、反向代理、生产 Redis 策略、MySQL/Redis 备份恢复、Outbox Worker/重试和外部 Provider 隔离验证。没有这些运行证据，不能把项目写成已完成生产部署或真实模型上线。
