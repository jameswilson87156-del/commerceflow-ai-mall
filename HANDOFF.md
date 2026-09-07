# Phase 0-B Handoff

## 2026-09-07 — RELEASE_CANDIDATE_LOCAL_REAUDIT

- 本次只做本地发布前复验；工作区原有修改和未跟踪文件均保留，没有执行 commit、push、Docker up/down、DNS、证书、云资源或数据库操作。
- Java 完整命令 `mvn -B test package` 结果为 91 tests、0 failures、0 errors、5 skipped、BUILD SUCCESS；5 条 `AiRateLimitRedisIntegrationTests` 因本机 `127.0.0.1:6380` 无监听按前置条件跳过。此前发现的 `@AfterEach` 清理误报已在测试层修复，Docker daemon 当前未运行，也没有启动或安装 Redis；Redis 集成实跑仍保持 `PENDING`。
- `services/ai-service`：`pytest -q` 为 11/11；`apps/admin-web`：`npm.cmd test` 为 46/46，`npm.cmd run build` 成功；`apps/mobile-app`：`npm.cmd test` 为 36/36，`npm.cmd run build` 与 `npm.cmd run build:uni` 成功。H5 构建保留既有 Vite CJS、`.env NODE_ENV` 和动态导入提示；三份前端生产依赖审计均为 0 high/critical vulnerabilities。
- CommerceFlow staging Compose 普通/TLS `config -q` 均退出 0；`.env.example` 被环境守卫按预期拒绝占位值，审计 fixture 正向通过。`git diff --check` 退出 0，仅有既存 PowerShell LF/CRLF 提示。
- 当前本地验收：`LOCAL_PASS`（除 Redis 集成外的 Java/Python/Admin/H5/Compose/隔离守卫）；Redis 集成 `PENDING`；阿里云主机、RDS/Tair、OIDC、DeepSeek Secret、DNS/ICP/TLS 和公网 staging 仍 `BLOCKED`/`STAGING_PENDING`。本地 `main` 比 `origin/main` 落后 2 个提交，工作区有 93 个已跟踪状态条目和 82 个未跟踪状态条目；尚未形成可推送 release commit。

## 2026-09-05 — STAGING_PHASE_A_AUDIT

- 当前总状态 BLOCKED；最新本地检查、整改和空库验证为 LOCAL_PASS，云端 STAGING_PENDING。Java `mvn -B test package` 91/91、0失败/错误/跳过；Python11/11、Admin46/46、H5 36/36；生产构建、npm生产依赖审计、配置守卫、schema-only MySQL 和 Compose普通/TLS静态检查通过。
- 已修复 staging 部署资产中的共享 OIDC 源码遗漏、Flyway Demo 种子、RDS/Tair 固定容器连接、OIDC client 变量不一致、跨项目 smoke 默认来源，并递归排除构建上下文中的本地 Secret 文件；未执行公网部署、购买、DNS/证书变更或数据删除。
- 阿里云控制台跳转登录页，账号资源/ICP/证书 BLOCKED；公开DNS查询不是资源所有权证据。保留所有既有代码修改，不购买、不改DNS、不部署、不提交或推送。
- 完整独立报告：[STAGING_AUDIT_PLAN_20260905](docs/release/STAGING_AUDIT_PLAN_20260905.md)。日志：D:/workhome/deployment-audit-20260905-1800/commerce-*。下文历史测试数量/未连接Provider表述按当时范围理解，当前以此审计及其标明的历史证据边界为准。
- 本轮整改复验汇总：`D:/workhome/deployment-remediation-20260905/REMEDIATION_RESULTS.md`；源码/产物哈希清单已同步到 `D:/workhome/deployment-audit-20260905-1800/`。
- 用户提供的阿里云截图显示华东1（杭州）轻量应用服务器 `OpenClaw-astw` 与 `wzl8.top`“已备案”；两者仅作为只读候选证据，未确认主机可占用、未修改 DNS、未启动项目。
- 2026-09-06 Workbench 只读检查显示主机 Docker 24.0.9 active、约 1.0 GiB 可用内存，80/443 已被现有服务占用，且有长期运行的 `searxng` 容器；主机作为两个项目 staging 目标为 BLOCKED，不停止或覆盖现有 OpenClaw/SearXNG 服务。

## Current update — 2026-09-05 — PHASE_2_REAL_DEEPSEEK_SYNTHETIC

- 已用 GitHub/官方文档调研得到的 Provider 边界、结构化输出和可观测性原则，补充 `scripts/local/verify-deepseek-synthetic.ps1`；脚本只接受本地未跟踪环境文件，把 API Key 注入临时 Java API 进程，不写入前端、源码、日志或证据。
- CommerceFlow 真实 DeepSeek 合成验收通过：`provider=deepseek`、`model=deepseek-chat`、`mode=REAL_OPENAI_COMPATIBLE`、`fallbackUsed=false`、`ANSWERED`，Java-owned Evidence `7` 条、Trace `6` 步。
- 脱敏证据：`docs/evidence/deepseek-synthetic-smoke-20260905.md`。临时端口已释放，临时日志未发现 Provider Key。
- Phase 2 只证明真实 Provider 成功路径；真实 IdP、staging 失败矩阵、云端备份恢复、DNS/TLS、监控和公网部署仍未完成。

## Current update — 2026-09-05 — PHASE_1_LOCAL_ACCEPTANCE

- 本地 Phase 1 已收口：真实本地 MySQL 8.4、Redis 8.0.2、Flyway 和 Java API Showcase 链路已运行验收；另用全新隔离 Compose 项目验证空 MySQL → Flyway 11 → readiness/API。Java 89/89（无跳过）、Python 11/11、Admin 46/46、H5 36/36，构建和 Compose 校验通过。
- `scripts/showcase/verify.ps1` 已修复限流 smoke 的状态污染：只删除配置 key 前缀，不执行 `FLUSHDB`；订单幂等、库存执行证据和 5 次成功 + 第 6 次 429 通过。
- 双项目总路线见 `D:/workhome/PROJECT_COMPLETION_ROADMAP_20260905.md`。真实 Provider、真实 IdP、云端备份恢复、DNS/TLS 和公网部署仍保持未验收。

## Current update — 2026-09-05 — FRONTEND_CRAFT

- 按用户要求精修现有前端。商城参考服装/生活方式陈列与清晰的购买交互，Admin 采用灰绿运营工作台；案例取舍、色板、布局、实现边界写入 docs/design/FRONTEND_CRAFT.md。
- Admin 新增 src/craft.css、线性导航 SVG、可展开数据说明和运行边界，取消写死的 Java API 在线结论。H5 新增 styles/craft.css、编辑式目录首屏、统一商品网格、桌面左右商品详情、两列规格、主次购买按钮与端侧导航适配。
- Admin/H5 npm run build 通过；Admin npm test 46/46，H5 npm test 36/36。D:/workhome/frontend-craft-qa.cjs 最终 53 组跨项目页面/状态通过，包含搜索、SKU、缺货禁用、加入购物车请求字段和进入确认页；详情见 D:/workhome/frontend-craft-20260905/验收报告.md。
- Admin/H5 成功态为浏览器脚本隔离的合成 API fixture，截图有水印；没有添加生产 Mock、写真实订单或调用付费模型。普通 5296/5297 开发地址仍请求配置的 API，不默认提供合成数据。
- 未修改后端、数据库、身份与 deploy 配置，未 commit/push/deploy，保留已有未提交修改。H5 构建现存 CJS/NODE_ENV/动静态导入提示仍存在，APP/小程序未运行验收。

## Current update — 2026-09-04 — SECURITY_HARDENING_AND_RELEASE_GATES

- Staging/production 启动守卫现在拒绝 Demo 身份、legacy 路由、local fallback、非 `FAIL_CLOSED` 限流、占位数据库配置、缺失/不安全的 rate-limit HMAC secret、缺失 Provider 配置和非 HTTPS CORS。SpringDoc/Swagger 在公开 profile 关闭。
- Nginx edge 已加入 CSP、`nosniff`、禁止 frame/object、Referrer/Permissions Policy；Caddy TLS profile 加入 HSTS。`/api/actuator/**` 不再公开反代，外部只保留最小 `/health`，Java readiness 留在内部网络。
- staging Compose 强制要求 `AI_RATE_LIMIT_IDENTITY_HASH_SECRET`、Provider/数据库/OIDC 等真实配置；MySQL/Redis 不公开宿主机端口。没有添加真实密钥、没有启动 Docker 或部署公网。
- Mobile 依赖更新后，补齐了 uni-app H5 compatibility layer 的 `injectHook` 转出；两条正式构建链路均通过。CI 现在审计 Admin/Mobile production dependency，并静态校验 staging Compose；Mobile 安装继续显式使用 `--legacy-peer-deps`，原因是当前 DCloud alpha 插件把 Vite peer 固定为旧的精确版本。
- 验证：Java 89 tests（0 failures/errors，5 Redis-only tests 因本机 Redis 不可用而按前置条件跳过）；Admin 46/46；Mobile 36/36；三个 browser package 的 production dependency audit 均为 0 vulnerabilities；Compose config 通过。没有真实 IdP/Provider 成功响应、云/SSH 凭据、commit、push 或 deploy。

## Current update — 2026-09-04 — REAL_AUTH_PROVIDER_DEPLOYMENT_PREPARATION

- 在保留工作区原有未提交修改的前提下，补齐 Java API 的可选 OIDC/JWT Resource Server、消费者 external subject 映射、Operator 角色边界，以及 Admin/H5 的 Authorization Code + PKCE 适配。Staging/production profile 选择 OIDC、关闭 Demo 身份和 legacy 路由。
- 真实 Provider 路径使用 Java OpenAI-compatible adapter，可通过环境变量切换 OpenAI、DeepSeek 或兼容网关。`COMMERCEFLOW_AI_FALLBACK_ENABLED=false` 现在真正生效：上游失败会返回 `PROVIDER_ERROR` 并记录 trace，不会静默生成本地答案；新增回归测试覆盖该语义。
- `deploy/staging/` 已收敛为 MySQL + Redis + Java API + Nginx edge + 可选 Caddy TLS；真实 Provider staging 不再启动 Python placeholder 服务。新增 [真实认证、Provider 与部署说明](docs/REAL_AUTH_PROVIDER_DEPLOYMENT.md)。Compose 配置静态校验通过，但 Docker daemon 当前不可用，因此没有 build/up 或公网部署。
- Java 回归：86 条测试被发现，其中 81 条可执行测试通过，5 条 Redis-only 测试因本机 `127.0.0.1:6380` 不可用而按前置条件跳过，Maven BUILD SUCCESS；Admin 46/46、Mobile 36/36，两个前端生产构建均通过。
- 没有真实 IdP token、有效 Provider 账户响应或云/SSH 凭据；共享兼容 Provider 验证返回 HTTP 403，不能记录为真实模型成功。没有执行 reset、checkout、clean、commit、push 或 deploy。

## Current scope

Phase 0-B only covers `D:/workhome/commerceflow-ai-mall`: consumer `/api/v1/me` scope, Operator cross-user management scope, Operations Overview authorization, runtime mode isolation, frontend/backend contract tests, and documentation. The enterprise ticket project is out of scope and was not modified.

## Implemented boundary

- Consumer cart, orders and AI wrappers use `/api/v1/me/...`; no client `userId` is sent in query/body.
- `CurrentUserPort` remains the only consumer identity dependency for versioned consumer controllers.
- Cross-user orders, execution evidence, Operations Overview and admin AI use `/api/v1/operator/...` and a separate `OperatorScope` plus `OperatorAuthorizationPolicy`.
- Legacy `/api/...` routes have no default `userId`, require the explicit legacy flag, and the flag is effective only in LOCAL/DEMO modes. Legacy management reads also require Operator authorization.
- Base runtime configuration is fail-closed (`NONE`, zero IDs, legacy disabled). `application-demo.yml`, `.env.example`, and test resources explicitly opt into local/test fixtures. Staging and production profiles use external identity modes, zero Demo IDs, legacy disabled, and AI rate-limit fail closed.
- Frontend error states distinguish Unauthorized, Forbidden, Not Found and Backend Unavailable. Real backend failures do not become Mock successes.

## Final verification observed

- Mobile Node tests: 36/36 passed, including the 401/403/5xx/network failure classification contract.
- Admin Vitest tests after the final authorization-state coverage: 46/46 passed.
- Mobile production build: passed; uni-app emitted existing environment-variable and dynamic-import notices.
- Admin type-check and production build: passed.
- Targeted Java `Phase0BBoundaryApiTests` plus `ShowcaseRuntimePolicyTests`: 8/8 passed on the final source.
- Full Java regression: `84` tests, `0` failures, `5` errors; all five errors are `AiRateLimitRedisIntegrationTests` failing to connect to unavailable local Redis `localhost:6380`. The remaining `79` tests were rerun with that named external integration class excluded and passed `79/79` with `BUILD SUCCESS`.
- `git diff --check`: no whitespace errors; Git may still print the existing LF/CRLF normalization warnings for PowerShell scripts.

## Important non-claims

This phase does not implement password login, Session, JWT, OIDC/OAuth2, real RBAC/ABAC, real model/API-key acceptance, public deployment, DNS/TLS, payment, logistics or production readiness. No Git reset/checkout/clean/delete/overwrite/commit/push was performed.

## Relevant documents

- `docs/design/PHASE_0B_API_BOUNDARY_AUTHORIZATION.md`
- `docs/evidence/PHASE_0B_POSTURE_ASSESSMENT.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
