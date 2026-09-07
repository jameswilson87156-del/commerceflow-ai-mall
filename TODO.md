# TODO / Remaining Phase 0-B Follow-up

## 2026-09-07 — GITHUB_RELEASE_BRANCH_CI

- [x] `LOCAL_PASS`：release 分支已推送；CommerceFlow CI run `34137998966` 的 6 个作业全部通过，包含 repository-integrity、Java + MySQL Flyway smoke、Python、Admin、Mobile 和 staging Compose。
- [x] `LOCAL_PASS`：修复首次 CI 暴露的容器路径误报、审计文档私网地址和旧 operations smoke 路由；修复提交为 `7c359c1`。
- [ ] `BLOCKED`：候选代码相对 `origin/main` ahead 1 / behind 2；GitHub `main` 尚未合并，需单独审阅后再做合并决定。

## 2026-09-07 — LOCAL_RELEASE_CANDIDATE_COMMITTED

- [x] `LOCAL_PASS`：在 `release/commerceflow-local-20260907` 形成候选提交 `14fa84833524c7081c2607410c57b8bcfe647858`；源码、测试、部署模板、文档和安全 `.env.example` 已纳入，真实 `.env`、本地运行配置、构建产物和临时目录未纳入且保留。
- [ ] `BLOCKED`：候选代码提交与 `origin/main` 的 ahead 1 / behind 2 差异需在合并前单独审阅；未执行 merge、云资源、DNS、证书、数据库或公网 staging。
- [ ] `PENDING`：恢复本机 Redis `127.0.0.1:6380` 后重跑 5 条 `AiRateLimitRedisIntegrationTests`。

## 2026-09-07 — RELEASE_CANDIDATE_LOCAL_REAUDIT

- [x] `LOCAL_PASS`：Python 11/11、Admin 46/46、H5 36/36；Admin/H5/UniApp 生产构建成功；三份前端生产依赖审计为 0 high/critical；Compose 普通/TLS 静态配置和正向隔离 fixture 通过。
- [x] `LOCAL_PASS`：Java 完整 `mvn -B test package` 为 91 条、0 failures、0 errors、5 skipped、JAR 构建成功；Redis 不可用时测试按前置条件跳过，清理误报已修复。
- [ ] `PENDING`：恢复本机 Redis `127.0.0.1:6380` 后重跑 5 条 `AiRateLimitRedisIntegrationTests`；当前 Docker daemon 未运行，不自动启动或安装服务。
- [ ] `BLOCKED`/`STAGING_PENDING`：镜像 digest/SBOM、阿里云资源/Secret/OIDC/RDS/Tair/DNS/ICP/TLS 和公网 staging；等待独立资源与用户确认。

## 2026-09-05 — STAGING_PHASE_A_AUDIT

- 2026-09-06 Workbench 只读审计：现有阿里云轻量应用服务器约 1.0 GiB 可用内存，80/443 已被既有服务占用，并运行长期 SearXNG/Node/Nginx；作为两个项目共用 staging 主机记为 `BLOCKED`，不得停止、覆盖或重配置既有服务。
- [ ] `BLOCKED`：选择容量和端口隔离均合适的独立 staging 主机，或在明确批准后形成扩容/隔离方案；在此之前不执行公网部署。
- 本轮本地测试/应用构建/配置守卫/空库初始化/Compose静态校验：LOCAL_PASS；当前部署总状态：BLOCKED。详见 [独立审计计划](docs/release/STAGING_AUDIT_PLAN_20260905.md)。最新 Java91/91、Python11/11、Admin46/46、H5 36/36；旧的Redis不可用记录为历史环境记录。
- [x] LOCAL_PASS：完成 C1–C5 的本地部署资产整改（共享 OIDC、schema-only 迁移、RDS/Tair 变量、双 client、项目内 smoke、Secret 构建上下文）；未做云端连接或公网部署。
- [ ] BLOCKED/STAGING_PENDING：完成 C6/C7 的干净 release、镜像 digest/SBOM、Docker 容器构建与云端资源/Secret/OIDC/DNS/ICP/TLS 验收。
- [ ] BLOCKED：用户自行登录后只读盘点云资源、DNS管理权、ICP、证书和正式IdP。
- [ ] STAGING_PENDING：实际报价和外部操作确认、云部署、真实Provider失败矩阵、备份恢复、浏览器、监控及回滚；不引用本地历史smoke为云验收。

## 2026-09-05 — FRONTEND_CRAFT

- [x] 目录/商品详情编辑式精修，Admin 灰绿工作台与导航、指标、数据说明精修，共享控件统一。
- [x] Admin/H5 构建通过，Admin 46/46、H5 36/36 测试通过，跨项目 53 组浏览器页面/状态回归通过。
- [x] 设计与验收落档：docs/design/FRONTEND_CRAFT.md；D:/workhome/frontend-craft-20260905/验收报告.md。
- [x] 本地真实 MySQL/Redis 与真实 DeepSeek 合成成功路径已通过；真实 IdP、staging 失败矩阵、云端备份恢复、DNS/TLS 与公网部署仍需独立验收。

## 2026-09-05 — PHASE_1_LOCAL_ACCEPTANCE

- [x] 完成本地 Phase 1：最新 Java `mvn -B test` 为 91/91；另用全新隔离的 MySQL 8.4 + Redis 8.0.2 Compose 项目完成空 MySQL → Flyway 11 → readiness/API 验收；Python 11/11；Admin 46/46；H5 36/36；生产构建、配置守卫、schema-only MySQL 和 Compose 静态校验通过。
- [x] `scripts/showcase/verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke` 通过 readiness、商品/SKU、AI Evidence/Trace、订单幂等/冲突 409、库存证据和 5+1 限流边界。
- [x] Redis 测试改为随机前缀隔离并只清理自己的 key；真实 Provider、真实 IdP、云端 MySQL/Redis、DNS/TLS 与公网部署仍未验收。
- [x] 双项目总路线见 `D:/workhome/PROJECT_COMPLETION_ROADMAP_20260905.md`，后续按 Phase 2 → Phase 3 → Staging → 公网顺序推进。

## 2026-09-05 — PHASE_2_REAL_DEEPSEEK_SYNTHETIC

- [x] 新增 `scripts/local/verify-deepseek-synthetic.ps1`，通过真实 `deepseek-chat` 访问 Java OpenAI-compatible adapter；Key 只进入临时后端进程。
- [x] 真实 Provider 运行结果：`REAL_OPENAI_COMPATIBLE`、`fallbackUsed=false`、`ANSWERED`；Java-owned Evidence `7` 条、Trace `6` 步。
- [x] 脱敏证据写入 `docs/evidence/deepseek-synthetic-smoke-20260905.md`；临时端口已释放，临时日志未发现 Provider Key。
- [ ] 真实 IdP token、staging 失败矩阵、云端备份恢复、DNS/TLS 和公网部署仍未完成。

The code paths below are implemented where marked, but external acceptance still requires real infrastructure and credentials:

- [x] Add CI production-dependency audit gates for Admin/Mobile and staging Compose interpolation validation. Public-profile startup configuration is now guarded against Demo/fallback/placeholder or incomplete secrets.
- [x] Stop public Nginx proxying of `/api/actuator/**`; retain only edge `/health` while internal readiness remains available to the service network.
- [x] Implement the real consumer identity adapter (OIDC/OAuth2/JWT) behind `CurrentUserPort`; independently validate it with a real IdP token before calling it production-ready.
- [x] Implement real Operator/Admin authentication and role mapping behind `OperatorScope`; independently validate tenant policy and audit subject handling with a real IdP token.
- Add staging IdP integration and 401/403 acceptance with real credentials; keep Demo identities forbidden in staging/production.
- Decide and execute a deprecation window for the legacy `/api` query/body `userId` routes, then remove them in a separately approved migration.
- Add automated route-level security checks to CI against staging-like configuration and verify no frontend bundle contains Demo identity defaults.
- Validate the full real provider/API-key failure matrix only with approved secrets and an isolated test account; one real DeepSeek synthetic success is now recorded in `docs/evidence/deepseek-synthetic-smoke-20260905.md`, while staging failure behavior remains pending.
- Complete staging backup/restore, monitoring/alerting, DNS/TLS and deployment review separately; the staging deployment scaffold is present, but no public deployment is implied by this phase.
- Resolve or explicitly provision the local Redis `localhost:6380` dependency before claiming the Redis integration portion of the full Java suite as green.
