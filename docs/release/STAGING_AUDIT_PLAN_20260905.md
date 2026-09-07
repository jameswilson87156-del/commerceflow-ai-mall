# CommerceFlow：第一阶段 staging 审计与部署计划

审计日期：2026-09-05（Asia/Shanghai）。本报告只适用于 `D:/workhome/commerceflow-ai-mall`。

**当前总状态：BLOCKED。** 本轮发现的部署资产问题已完成本地整改并复验；最新本地测试、应用生产构建、配置守卫、空库初始化和 Compose 静态检查为 LOCAL_PASS。云端资源、真实 Secret、OIDC、RDS/Tair 连通性、镜像构建与公网验收仍为 STAGING_PENDING 或 BLOCKED，不能据此宣称已部署。

## 1. 范围与当前工作区

- 已读取双项目总入口、本项目专用部署文档、README、HANDOFF、TODO、REAL_AUTH_PROVIDER_DEPLOYMENT、staging 资产、验证脚本及脱敏 Provider/OIDC 历史证据。
- 分支 `main`，HEAD `72e3dc5440a835c546c0f8506ccd3638b8f8ae43`。
- 审计开始时 93 个已跟踪文件有修改，80 个 untracked 状态条目（含目录，不等于文件总数）。认证代码、共享 OIDC、迁移 V9–V11 和部署资产等存在未跟踪内容。HEAD 不能代表本次测试的完整源码。
- `git diff --check` 为 LOCAL_PASS。本轮不提交、不推送；保留既有修改，仅对 staging/production 部署边界、配置守卫、迁移基线和相关文档做了可追溯的本地整改。
- 本机工具：Java 17.0.19、Maven 3.9.11、Node 24.16.0、Docker Compose 5.3.1。Dockerfile/CI 使用 Node 20，容器环境一致性仍需独立验证。
- 本机已有 MySQL 8.4、Redis 8.0.2；Redis 绑定 127.0.0.1:6380。电商本地 MySQL 映射为 0.0.0.0:3307 和 IPv6，不能把这个本地端口策略照搬到云端；未改变它。

## 2. 本轮实际验证

命令均在对应项目目录执行；日志在 `D:/workhome/deployment-audit-20260905-1800/`，前缀 `commerce-`。

| 验证 | 实际命令 | 结果与边界 |
| --- | --- | --- |
| Java 测试与可执行 JAR | apps/mall-api：`mvn -B test package` | LOCAL_PASS：91 tests，0 failures/errors/skipped；含 staging schema 防重放测试；JAR repackage 完成 |
| Python 既有 Mock 服务回归 | services/ai-service：`.venv/Scripts/python.exe -m pytest -q` | LOCAL_PASS：11/11；该服务不进入真实 Provider staging 拓扑 |
| Admin 测试 | apps/admin-web：`npm.cmd test` | LOCAL_PASS：46/46 |
| Admin 生产构建 | apps/admin-web：`npm.cmd run build` | LOCAL_PASS：含 vue-tsc，37 modules |
| H5 测试 | apps/mobile-app：`npm.cmd test` | LOCAL_PASS：36/36 |
| H5 两条构建 | apps/mobile-app：`npm.cmd run build`、`npm.cmd run build:uni` | LOCAL_PASS；存在 CJS API、NODE_ENV、动静态混合导入提示；不代表原生设备验收 |
| Admin/H5 生产 npm 依赖 | 各自：`npm.cmd audit --omit=dev --audit-level=high --json` | LOCAL_PASS：两份结果均 total=0；不是 Java/镜像漏洞结论 |
| Compose 普通配置 | 根目录：`docker compose --env-file deploy/staging/.env.example -f deploy/staging/docker-compose.yml config -q` | LOCAL_PASS：退出码 0，仅模板插值和配置语法 |
| Compose TLS 配置 | 上述命令加 `--profile tls`（置于 config 前） | LOCAL_PASS：退出码 0；未启动 TLS、未申请证书 |
| 环境守卫负向验证 | `scripts/staging/validate-env.ps1 -EnvironmentFile deploy/staging/.env.example` | LOCAL_PASS：按预期拒绝 RDS URL 占位符；不是运行配置通过 |
| 独立 staging 配置守卫正向验证 | 同一脚本读取审计目录内仅含假值的 commerce fixture | LOCAL_PASS：外部 MySQL/Tair、TLS、OIDC、双前端 client、CORS、fallback=false 和跨项目变量隔离规则通过；不代表远端可达 |
| 空库 schema-only MySQL 验证 | 隔离 `mysql:8.4`、无网络、无发布端口的临时容器 | LOCAL_PASS：12 个业务表为空，首次初始化成功，重复执行被拒绝；容器停止保留，未触碰用户数据库 |
| 真实 staging 环境文件 | deploy/staging/.env | BLOCKED：不存在；未借用工单项目环境文件 |
| Java 依赖/容器漏洞、完整 Docker 镜像构建与运行 | 未执行 | STAGING_PENDING；npm 审计不能代替这些检查 |

Java 数据库测试使用 H2，Redis 使用现有本地实例并限定随机测试 key 前缀。测试框架会清理自己的测试数据；未连接云数据库，未迁移或删除既有业务数据。没有执行 Compose up/down、真实订单 smoke 或外部付费模型请求。

## 3. 部署阻塞项与整改计划

| 编号 | 状态 | 实际发现 | 后续最小整改及验证 |
| --- | --- | --- | --- |
| C1 | LOCAL_PASS | 两个 edge build stage 已复制 `apps/shared/`，与各自 OIDC import 路径一致 | Dockerfile 静态路径已核对；完整镜像构建仍受 Docker Hub token 网络超时影响，记录为 STAGING_PENDING |
| C2 | LOCAL_PASS | staging/production 已切到独立 `db/staging-migration/V1__schema_only.sql` 与独立 history 表；不加载 Demo 种子 | Java 91/91 与隔离 MySQL 空库、防重放验证通过；云端新空库初始化和受审查合成数据仍 STAGING_PENDING |
| C3 | LOCAL_PASS | Compose 已移除 MySQL/Redis 服务及固定容器地址，改为显式 RDS/Tair 私网变量并要求 TLS 配置 | Compose 普通/TLS config 及 fixture 守卫通过；RDS/Tair endpoint、CA、白名单和连通性仍 STAGING_PENDING |
| C4 | LOCAL_PASS | Admin/H5 已使用独立 `VITE_ADMIN_AUTH_CLIENT_ID`、`VITE_MOBILE_AUTH_CLIENT_ID` build args | 本地参数隔离规则通过；正式 redirect、audience、角色、注销和 IdP 行为仍 BLOCKED |
| C5 | LOCAL_PASS | DeepSeek 本地 smoke 默认只读本项目 `deploy/staging/.env`，不再回退到工单变量 | 未读取或复制任何 Secret；真实服务器 Secret 和上游响应仍 BLOCKED |
| C6 | BLOCKED | 未提交/未跟踪源码不在 HEAD，镜像尚未锁定 digest | 纳入经过审查的源码、建立独立 release manifest、镜像 digest、迁移版本、配置版本和前一版本 |
| C7 | STAGING_PENDING | Java/前端本地回归和 npm 生产依赖审计已通过；Docker Hub token 请求超时，Java/镜像 SBOM 和 Linux/Node 20 容器构建尚未完成 | CI 迁移断言已校正为 11；待网络可用后完成镜像构建、digest/SBOM、容器运行与 schema-only 云端复验 |

两个项目若在同一 ECS 上照原 Compose 同时启动各自 Caddy，会争用宿主机 80/443；默认 Compose project name 还会由相同目录名 staging 推导。必须显式使用独立项目名（如 `commerceflow-staging`），不能依赖目录默认值。

## 3.1 本轮整改结果（2026-09-05）

以下改动已经写入工作区并在本地复验，范围只覆盖电商项目：

- `deploy/staging/docker-compose.yml` 不再创建或依赖本地 MySQL/Redis；API 通过 `COMMERCEFLOW_*` 变量连接外部 RDS/Tair，迁移账号和运行账号分开，Redis TLS、OIDC audience、限流 HMAC secret 设为显式门禁。
- `deploy/staging/edge.Dockerfile` 的 Admin/H5 两个构建阶段均复制 `apps/shared/`，并将两个独立 public client 映射到各自构建产物；`.dockerignore` 递归排除 `.env`、密钥和本地 profile 文件。
- staging/production profile 使用新的 `db/staging-migration` schema-only stream 和独立 history 表；原有 `db/migration/V1..V11` 未改写。隔离 MySQL 验证了 12 个业务表为空、重复初始化被拒绝。
- `deploy/staging/.env.example`、`scripts/staging/validate-env.ps1` 和本地 DeepSeek smoke 已收敛到本项目变量；fixture 正向校验通过，模板仍按预期被拒绝，不包含真实 Secret。
- 最新 `mvn -B test package` 为 91/91；Admin 46/46；H5 36/36；Admin/H5/JAR/Python 构建或回归通过。两次镜像构建在拉取 Docker Hub 匿名 token 时网络超时，属于 STAGING_PENDING，不归因于源码构建成功。

整改后的剩余门禁仍是外部条件：实际阿里云账号会话、ECS/RDS/Tair 资源、正式 OIDC、服务器 Secret、Docker registry 可达性、镜像 digest/SBOM、DNS/ICP/TLS 和公网验收。没有执行公网部署或数据库删除。

## 3.2 用户提供的阿里云盘点证据（2026-09-05）

- 轻量应用服务器页面显示：华东 1（杭州）、`OpenClaw-astw`、运行中、公网 `47.98.192.15`、私网 `172.25.5.238`、2 vCPU/2 GiB/40 GiB、到期 2026-12-26。该主机只能作为候选 staging 宿主机；在确认现有工作负载、监听端口、CPU/内存/磁盘余量、系统用户、Docker、轻量应用服务器防火墙和 SSH 方式前，不得覆盖或启动任何项目。
- 域名页面显示 `wzl8.top` 状态“正常”、备案“已备案”、到期 2027-06-26，并显示“添加域名解析”操作。该证据不授权本轮修改 DNS；此前四个 staging 子域名仍解析为 NXDOMAIN。
- 以上截图只更新资源盘点证据，不等于电商 staging 已部署、域名已解析、证书已签发或公网验收通过。

## 3.3 Workbench 只读主机审计（2026-09-06）

用户通过阿里云 Workbench 进入 `OpenClaw-astw`，并执行了不读取环境变量或 Secret 的只读检查。可确认：Alibaba Cloud Linux 4（Agentic Edition）、x86_64、Docker client/server 24.0.9 且服务 active、运行约 71 天、2 个 CPU、根盘 40 GiB 已用 11 GiB 可用 27 GiB。内存总量约 1.8 GiB，当前 available 约 1.0 GiB，swap 2 GiB。

当前只读监听结果包含 80、443、22，以及 127.0.0.1 上的 8080、18080、18443、35207，另有 Node 进程监听 18352。Docker 运行中的容器至少有 `searxng`（`searxng/searxng:latest`，已运行约两个月）；宿主机还有 Node、Nginx、Python 等现有进程。`firewalld` 和 `ufw` 服务显示 inactive，但阿里云轻量应用服务器的云侧防火墙规则尚未读取。

结论：该主机当前不能直接作为两个项目的 staging 目标。80/443 已被占用，约 1 GiB 可用内存不足以在未知现有负载下安全承载两个 Spring Boot API、双前端 Edge 和构建任务；现有 OpenClaw/SearXNG 服务也没有获得停机或覆盖授权。候选方案是单独准备容量更合适的主机，或由用户明确批准后先做资源扩容和共享入口设计；本轮不停止、重启、重配或删除现有服务。

## 4. 阿里云、域名与外部条件盘点

本机 PATH 未发现 aliyun CLI，常规 `~/.aliyun/config.json` 不存在，未发现可调用阿里云 connector。用户随后提供了阿里云控制台截图；截图属于用户提供的只读盘点证据，不能替代对实例详情、端口、防火墙、磁盘、VPC、RDS/Tair 和权限的逐项核实。

| 条件 | 本轮可确认内容 | 状态 |
| --- | --- | --- |
| ECS/地域/VPC/vSwitch/IP/SSH/安全组 | 截图和 Workbench 只读终端显示华东 1（杭州）轻量应用服务器 `OpenClaw-astw`，公网 `47.98.192.15`、私网 `172.25.5.238`、2 vCPU/约 1.8 GiB/40 GiB、Docker 24.0.9；当前可用内存约 1.0 GiB，80/443 已有监听，且存在现有 SearXNG/Node/Nginx/本地服务。它不是可直接占用的 ECS，主机防火墙、云侧规则、Docker 网络、端口归属和负载仍未完整核实 | BLOCKED |
| RDS MySQL | 实例、私网地址、应用账号、权限、白名单、备份及恢复均未确认 | BLOCKED |
| Tair/Redis | 云实例、私网地址、ACL/密码 Secret、白名单、TLS/兼容性、备份均未确认 | BLOCKED |
| 根域名公开 DNS | 2026-09-05 本机解析器查询：wzl8.top A=47.98.192.15；NS=dns7.hichina.com、dns8.hichina.com | LOCAL_PASS：仅 DNS 查询证据 |
| 电商 staging DNS | mall-staging、admin-staging、auth-staging 的 A/AAAA 查询返回 NXDOMAIN | STAGING_PENDING |
| DNS 管理权 | 域名控制台截图显示 `wzl8.top` 可在当前控制台管理并提供“添加域名解析”等操作；未点击、未修改记录，控制台主体与解析权限仍需详情页核实 | STAGING_PENDING |
| ICP | 域名控制台截图显示 `wzl8.top` 状态“正常”、备案“已备案”，到期 2027-06-26；备案主体、接入商、具体网站绑定和 staging 子域名关系尚未逐项核实 | STAGING_PENDING |
| TLS | 未看到证书资产/有效期/续期配置，未启动 ACME | BLOCKED |
| 正式 OIDC | HTTPS issuer、JWKS、audience、Admin/H5 clients、账户映射、MFA/生命周期未确认 | BLOCKED |
| 本地 OIDC 历史 | 已读取 local-oidc 的 PKCE 和 API 401/403/200 脱敏记录；本轮未重跑协议及浏览器流程 | STAGING_PENDING：不作为本轮或公网验收 |
| DeepSeek | 已读取本项目 2026-09-05 历史合成成功证据：ANSWERED、fallbackUsed=false、Evidence 7、Trace 6；本项目独立 staging Secret 尚未确认 | BLOCKED：本轮真实调用/云端 Secret 未验证 |

涉及中国内地 ECS 的对外 Web 服务，备案作为开放 staging 前置门禁，不能用“只是 staging”跳过。参考：[阿里云备案流程 FAQ](https://help.aliyun.com/zh/icp-filing/basic-icp-service/support/for-the-record-process-faq)。公开规则不能证明 wzl8.top 的具体备案状态。

## 5. 拓扑、隔离和费用计划

优先复用满足条件的既有资源；以下是待审查设计，不是购买清单或部署结果。

```text
mall-staging.wzl8.top / admin-staging.wzl8.top
  -> 本项目 ECS 的 Caddy/Nginx 80/443
  -> H5/Admin 静态站点 + mall-api 私有 Docker 网络
  -> 本项目数据库 commerceflow_staging（RDS MySQL，同地域/VPC）
  -> 本项目 Tair/Redis（私网，独立 ACL/Secret/key namespace）
  -> HTTPS 正式 IdP（Admin/H5 独立 public client）
  -> DeepSeek（仅 Java 读取本项目服务器 Secret）
```

- 默认规划独立 ECS，避免与工单抢占 80/443 或资源；若实际仅有一台可用 ECS，另设计一个共享宿主机入口，分别转发到 127.0.0.1:8088 与工单 8087，两个 app stack 禁用各自 public TLS profile。不得把两个数据库/环境文件合并。
- Workbench 只读结果已确认现有候选主机为轻量应用服务器而非 ECS，约 1.0 GiB 可用内存且 80/443 已占用；当前不把它视为两个项目的可部署目标，也不在其上覆盖现有服务。应优先使用独立且容量合适的主机；若用户考虑扩容/复用，必须先完成资源变更评估和两项目隔离设计。
- 建议容量起点：单实例 API，ECS 2 vCPU/4 GiB、40–60 GiB 盘；RDS MySQL 8.x、Redis 256–512 MiB 起评估。均为容量假设，需用现有资源和压测修订；不是已核实可购买 SKU。
- 公网仅入口 80/443，SSH 22 限用户固定 IP /32；3306/6379/API/本地认证端口不开放。RDS/Tair 同地域/VPC、私网与最小白名单。
- 计划运行目录 `/srv/commerceflow-staging`；独立 env/Secret、数据库用户、日志、合成数据、备份前缀和 release manifest。数据库迁移账号与运行账号分开；使用已有实例时也必须独立数据库和权限。
- 保持 `COMMERCEFLOW_AI_FALLBACK_ENABLED=false`、Redis `FAIL_CLOSED`；所有 TICKET_* / PORTFOLIO_* 变量不注入电商运行环境。
- **费用估算状态：STAGING_PENDING。** 未知地域、现有规格、计费周期、余量和折扣，不能可靠给出人民币报价。电商月增量公式：新增/扩容 ECS + 云盘 + 公网出流量/EIP + RDS + Tair + 超额备份/日志 + IdP（若收费）+ 本项目模型调用。复用也可能产生流量、备份和模型增量；不能写免费。
- 本轮无云资源创建/购买、无新模型调用。实际下单前逐项填写官方购物车报价、月上限和自动续费选项，获得用户明确确认。参考：[ECS 计费概述](https://help.aliyun.com/zh/ecs/billing-overview)、[公网带宽计费](https://help.aliyun.com/zh/ecs/public-bandwidth/)。

## 6. 风险与回滚

| 阶段/风险 | 上线前保护 | 回滚设计（尚未演练） |
| --- | --- | --- |
| 本地部署资产整改 | 小范围 diff；保留用户修改；建立完整源码版本 | 使用独立修订记录撤销本轮特定改动，不用 reset/checkout/clean |
| 镜像/配置升级 | 每版本保存 image digest、配置版本、DB schema 和验收记录；秘密仅记引用 | 以本项目上一 digest 和配置版本重建 API/edge；不重建数据库、不 down -v |
| 首次发布 | 新独立数据库，schema-only；使用合成数据 | 验收失败仅停止新 app，保留数据、镜像和日志；不得称有已验证前版 |
| 数据库升级 | additive/expand-contract，先备份到独立位置，恢复到临时库检查订单/库存/Outbox 一致性 | 旧代码兼容新 schema 时只回滚应用；不兼容时先停止写入，恢复到新库并核对增量，经确认切连接，不覆盖源库 |
| DNS/TLS | 先保存精确 A/AAAA/CNAME/TTL 和证书版本，备案与主机确认 | 只恢复本项目变更的记录/入口配置；任何 DNS 修改仍需用户确认，根域名不切换 |
| Redis/Provider 故障 | 限流 fail-closed、fallback=false，错误告警与成本上限 | 降低或暂停 AI 入口，修复连接/回退已验证配置；不靠关闭认证、TLS、限流制造成功 |
| 资源费用 | 标记项目归属与预算，采购前确认 | 无状态资源停止/释放也须确认范围；停止实例不意味着所有存储/IP费用终止，数据资源不删除 |

临时目标 RPO ≤24h、RTO ≤60min 仅为演练设计值，未验证、不是承诺。后续必须记录备份时间、恢复时长、行数/约束和业务校验结果。

## 7. 分项目验收与持续升级

以下云端项目全部 STAGING_PENDING：DNS/HTTPS及续期；外部 /health 与内部 readiness 隔离；匿名401；消费者只读写本人资源；消费者访问运营403；Operator成功；正式 OIDC/PKCE 刷新/退出/MFA；DeepSeek ANSWERED/Evidence/Trace；Provider 401/429/5xx/超时/非法JSON失败矩阵；订单幂等、库存不足事务回滚、快照及 Outbox；Admin/H5浏览器；MySQL临时库恢复；Redis私网/白名单/fail-closed；日志/前端Secret检查；监控告警与应用回滚。

顺序：先完成 C6/C7 的干净 release、镜像 digest/SBOM 和容器复验 → 完成只读资源盘点 → 形成实际费用和变更单 → 用户确认相关云操作 → 私网内部部署与 readiness → 经确认 DNS/TLS/公网 staging → 完整验收 → P0 正式身份/备份/监控/回滚 → P1 Outbox dispatcher/重试/死信/重放、失败矩阵、评测与成本 → P2 支付/物流等真实闭环。当前不创建无人值守升级或自动部署任务。

## 8. 用户准备的最小信息

先只需要一个动作：在已打开的阿里云登录页面由用户自行完成登录，并告知可只读盘点；不发送 Cookie、Token、密码或验证码。

后续尽量从控制台读取，仅补不可推断项：地域与本项目资源归属；SSH 用户/本地私钥路径和可信运维 IP（仅路径，不是私钥）；根域名管理权与 ICP 页面状态；独立数据库名/用户/私网 endpoint；本项目 Secret 存放位置（仅路径或标识）；正式 issuer/audience/Admin/H5 client 与回调；测试用户准备方式；月预算上限。未指定时保持 BLOCKED，不使用工单输入填补。
