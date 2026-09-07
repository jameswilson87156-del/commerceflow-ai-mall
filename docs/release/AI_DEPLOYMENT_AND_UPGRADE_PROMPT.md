# CommerceFlow 阿里云 Staging 部署与持续升级交接文档

适用项目：D:/workhome/commerceflow-ai-mall

目标：把当前本地已经验收的电商项目部署到阿里云 staging 环境，再按照验收结果持续升级。

生成日期：2026-09-05

重要边界：本文件是部署交接和给下一个 AI 的执行提示词，不代表项目已经部署到公网。任何购买资源、修改 DNS、申请证书、导入真实数据、删除数据、切换正式域名或产生费用的操作，都必须先停下来获得用户明确确认。

---

## 1. 当前项目真实状态

### 已完成

- 商品、SKU、购物车、订单和库存核心链路已完成本地验收。
- Java API、Admin Web、Mobile/H5 已有本地测试和生产构建。
- 订单幂等、库存边界、订单快照、库存流水和事务性 Outbox 已实现。
- Admin 和 H5 的错误、加载、空数据、未授权、无权限和后端不可用状态已补齐。
- Java 服务端已经具备 OpenAI-compatible Provider 边界，可切换 DeepSeek。
- 真实 DeepSeek 本地合成请求已成功：

~~~text
Provider: deepseek
Model: deepseek-chat
Runtime: REAL_OPENAI_COMPATIBLE
fallbackUsed: false
Answer: ANSWERED
Java-owned Evidence: 7
Trace: 6 steps
~~~

- 本地临时 Keycloak 已完成 OIDC/PKCE 协议级验收：
  - mall-customer：commerceflow-api audience、customer role、user_id=1。
  - mall-operator：commerceflow-api audience、operator role、operator_id=9001。
- 本地 API 权限边界已验证：
  - 未登录消费者接口返回 401。
  - 消费者访问运营接口返回 403。
  - 运营者访问运营总览返回 200。

### 尚未完成，不能写成已上线

- 尚未完成阿里云 ECS staging 的真实部署。
- 尚未完成云端 MySQL / Redis 的备份和恢复演练。
- 尚未完成正式 OIDC/IdP、真实用户生命周期和 MFA 验收。
- 尚未完成 HTTPS 域名下的消费者和运营端浏览器验收。
- 尚未完成 staging 环境下的真实 Provider 失败矩阵。
- 尚未完成正式域名 DNS、TLS、监控、告警和回滚验收。
- 当前本地 Keycloak 只用于隔离测试，不能直接暴露到公网。
- 当前本地 Demo 用户、H2、SHOWCASE_MODE 和本地测试账号不能进入 staging/production。

### 证据位置

- 真实 DeepSeek：docs/evidence/deepseek-synthetic-smoke-20260905.md
- 本地 OIDC/PKCE：D:/workhome/local-oidc/PKCE_SMOKE_EVIDENCE_20260905.md
- 本地身份边界：D:/workhome/local-oidc/OIDC_SMOKE_EVIDENCE_20260905.md
- 认证、Provider 和 staging 说明：docs/REAL_AUTH_PROVIDER_DEPLOYMENT.md
- 项目交接：HANDOFF.md
- 任务清单：TODO.md

如果仓库文档和最新可复现证据冲突，以最新测试输出和实际配置为准；交付前必须修正文档，不得选择有利结论汇报。

---

## 2. 推荐的阿里云 staging 拓扑

第一阶段只做一个可回滚的 staging 环境，不直接切换正式域名。

~~~text
mall-staging.<DOMAIN>
admin-staging.<DOMAIN>
        |
        v
      ECS Edge
  Nginx / Caddy
  80 / 443
        |
        +-- Mobile/H5 静态资源
        +-- Admin 静态资源
        +-- Java Mall API
                 |
                 +-- RDS MySQL：同地域、同 VPC、私网连接
                 +-- Tair/Redis：同地域、私网连接、白名单
                 +-- 正式 OIDC/IdP：HTTPS issuer
                 +-- DeepSeek：服务端 API Key
~~~

推荐原则：

1. ECS 只对公网提供 Edge 的 80/443。
2. SSH 22 只允许用户自己的固定公网 IP。
3. MySQL 3306、Redis 6379、Java 内部端口、认证内部端口不对公网开放。
4. ECS、RDS、Redis 优先选择同地域、同 VPC，应用走私网地址。
5. staging 使用子域名，例如：

~~~text
mall-staging.wzl8.top
admin-staging.wzl8.top
auth-staging.wzl8.top
~~~

最终真实主域名要等 staging 全部验收通过后再讨论。

---

## 3. 用户需要准备的输入

下一个 AI 不允许要求用户把密码、私钥、API Key 粘贴到聊天中。

### 阿里云资源

~~~text
ALIYUN_REGION
ECS_INSTANCE_ID
ECS_PUBLIC_IP
ECS_PRIVATE_IP
ECS_SSH_USER
ECS_SSH_KEY_PATH
ECS_VPC_ID
ECS_VSWITCH_ID
~~~

如果 ECS 还不存在，下一个 AI 必须先报告预计资源、预算和购买动作，等待用户明确确认后才能创建。

### 域名

~~~text
ROOT_DOMAIN=wzl8.top
STAGING_MOBILE_HOST=mall-staging.wzl8.top
STAGING_ADMIN_HOST=admin-staging.wzl8.top
STAGING_AUTH_HOST=auth-staging.wzl8.top
DNS_PROVIDER=aliyun-or-other
ICP_STATUS=completed|in_progress|not_started|unknown
~~~

如果域名不在当前阿里云账号中，先检查 DNS 管理权，不得擅自修改原注册商的 Nameserver。

### 数据库和 Redis

~~~text
RDS_INSTANCE_ID
RDS_PRIVATE_ENDPOINT
RDS_DATABASE_NAME
RDS_APPLICATION_USERNAME
RDS_APPLICATION_PASSWORD_SECRET_LOCATION
REDIS_INSTANCE_ID
REDIS_PRIVATE_ENDPOINT
REDIS_PASSWORD_SECRET_LOCATION
~~~

数据库密码和 Redis 密码必须通过服务器 Secret、阿里云密钥管理或用户指定的未提交环境文件注入。

### OIDC

~~~text
COMMERCEFLOW_AUTH_ISSUER_URI
COMMERCEFLOW_AUTH_AUDIENCE
COMMERCEFLOW_AUTH_ROLES_CLAIM
VITE_MOBILE_AUTH_CLIENT_ID
VITE_ADMIN_AUTH_CLIENT_ID
OIDC_REDIRECT_URIS
OIDC_TEST_USER_PROVISIONING_PLAN
~~~

浏览器 Client 使用 Authorization Code + PKCE（S256），不把 Client Secret 放入前端。

### AI Provider

DeepSeek Key 已经准备过，不需要再次发送给 AI。下一个 AI 只需要确认它存在于服务器端 Secret 中：

~~~text
COMMERCEFLOW_AI_PROVIDER=OPENAI_COMPATIBLE
COMMERCEFLOW_AI_BASE_URL=https://api.deepseek.com
COMMERCEFLOW_AI_PATH=/v1/chat/completions
COMMERCEFLOW_AI_MODEL=deepseek-chat
COMMERCEFLOW_AI_API_KEY=<server-side-secret>
COMMERCEFLOW_AI_FALLBACK_ENABLED=false
~~~

日志、前端 bundle、Git、截图和测试输出中不得出现 Key。

---

## 4. 阿里云控制台准备顺序

### Step 0：先盘点，不购买

检查以下控制台：

1. ECS：实例、地域、VPC、私网 IP、公网 IP。
2. 云解析 DNS：是否管理 wzl8.top。
3. ICP 备案：已备案、备案中还是未备案。
4. RDS：是否已有 MySQL 实例。
5. Tair/Redis：是否已有 Redis 实例。
6. 证书：是否已有 staging 域名证书。

没有完成盘点前，不得直接购买多个云产品。

### Step 1：ECS

如果没有 ECS，创建前必须先确认：

- 地域。
- VPC。
- Linux 镜像。
- 公网 IPv4/EIP。
- SSH Key Pair。
- 实例规格和磁盘。
- 计费方式、预算和自动续费。

优先使用 SSH 密钥和普通运维用户，不把 root 密码作为长期登录方案。

官方文档：

https://help.aliyun.com/zh/ecs/user-guide/create-an-instance-by-using-the-wizard/

### Step 2：安全组

ECS 入方向最终只允许：

~~~text
TCP 22   用户固定公网 IP /32
TCP 80   0.0.0.0/0
TCP 443  0.0.0.0/0
~~~

不要开放：

~~~text
3306、6379、8080、8180、5295、5296、5297、5298
~~~

官方文档：

https://help.aliyun.com/zh/ecs/user-guide/start-using-security-groups

### Step 3：RDS MySQL

如果使用 RDS：

1. RDS 和 ECS 放在同地域、同 VPC。
2. 数据库版本和项目迁移版本兼容，优先保持 MySQL 8.x。
3. 只开启私网连接。
4. 白名单只允许 ECS 私网 IP 或指定安全组。
5. 创建独立应用账号，不使用 root 连接应用。
6. 确认自动备份、保留时间和恢复方式。
7. 不把 RDS 外网地址写入前端或 Git。

官方文档：

https://help.aliyun.com/zh/rds/support/how-do-i-connect-to-an-apsaradb-rds-instance

### Step 4：Tair/Redis

如果使用阿里云 Tair/Redis：

1. 和 ECS 放在同地域、同 VPC。
2. 使用私网连接地址。
3. 白名单只添加 ECS 私网 IP 或 ECS 安全组。
4. 设置强密码或官方支持的认证方式。
5. 不把 Redis 6379 暴露给公网。
6. 验证应用能连接后，再启用项目的 AI rate limit。

官方文档：

https://help.aliyun.com/zh/redis/user-guide/configure-whitelists

### Step 5：DNS

staging 先添加：

~~~text
类型：A
主机记录：mall-staging
记录值：ECS 公网 IP

类型：A
主机记录：admin-staging
记录值：ECS 公网 IP
~~~

如果认证服务由外部 IdP 提供，auth-staging 由 IdP 的实际域名决定，不要擅自指向 ECS。

官方文档：

https://help.aliyun.com/zh/dns/pubz-add-website-parsing

### Step 6：ICP备案判断

如果 ECS 在中国内地，域名正式对外提供访问前需要完成 ICP 备案；如果还没有备案，先不要把正式域名直接切到中国内地 ECS。

官方入口：

https://beian.aliyun.com/

官方说明：

https://help.aliyun.com/zh/icp-filing/basic-icp-service/getting-started/quick-start-for-icp-filing-for-personal-websites

### Step 7：HTTPS

可选方案：

1. staging 使用项目已有 Caddy TLS profile，让 Caddy 通过 ACME 自动申请证书。
2. 使用阿里云数字证书管理服务申请证书，再部署到 Nginx/Caddy。
3. 正式环境必须确认自动续期和证书到期告警。

证书私钥只能放服务器 Secret 路径，不能提交 Git、发到聊天或放进前端。

官方文档：

https://help.aliyun.com/zh/ssl-certificate/download-an-ssl-certificate

---

## 5. 给下一个 AI 的可复制执行提示词

下面整段可以直接复制给下一个 AI。

~~~text
你是 CommerceFlow 电商项目的部署、发布、认证、安全和持续升级负责人。

项目目录：
D:/workhome/commerceflow-ai-mall

目标：
先把项目部署到阿里云 staging，不直接上线正式域名；staging 验收通过后，再按 TODO 持续升级。

必须先做：
1. 读取 README.md、HANDOFF.md、TODO.md、docs/REAL_AUTH_PROVIDER_DEPLOYMENT.md、docs/evidence、deploy/staging 和 scripts/staging。
2. 检查当前工作区，不得使用 git reset --hard、git checkout --、git clean、删除用户文件或覆盖既有未提交修改。
3. 运行本地测试、构建、Compose 静态校验和依赖审计，记录真实结果。
4. 先输出部署计划，列出已通过、未完成、需要用户提供的资源、预计费用、每一步回滚方式和需要用户确认的动作。
5. 在用户确认前，不购买 ECS/RDS/Redis/证书，不改 DNS，不切换正式域名，不删除数据，不执行破坏性迁移。

当前真实边界：
- 本地真实 DeepSeek 合成验收已通过，但不等于公网模型质量、成本、并发或生产 SLA 已通过。
- 本地 OIDC/PKCE 和 API 401/403/200 边界已通过，但不等于正式 IdP、正式用户生命周期、MFA 和公网浏览器验收已通过。
- H2、Demo 用户、SHOWCASE_MODE、local Keycloak 和本地测试密码不得进入 staging/production。
- 不能把本地协议 smoke 写成公网浏览器验收。

推荐阿里云拓扑：
- 一个 ECS 运行 Nginx/Caddy、Admin 静态资源、Mobile/H5 静态资源和 Java API。
- RDS MySQL 与 ECS 同地域同 VPC，应用使用私网连接。
- Tair/Redis 与 ECS 同地域同 VPC，使用私网连接和白名单。
- 真实 OIDC 使用 HTTPS issuer。
- DeepSeek API Key 只注入 Java 服务端 Secret。
- 公网只暴露 80/443。
- SSH 22 只允许用户固定公网 IP。
- MySQL、Redis、Java 内部端口和认证内部端口不暴露公网。

安全规则：
- API Key、数据库密码、Redis 密码、SSH 私钥、OIDC Token 不能出现在命令输出、日志、截图、前端 bundle、Git 或聊天内容中。
- 不要自动登录第三方控制台或替用户购买付费资源。
- 不要使用不明来源的第三方部署脚本。
- 不要因为部署失败而关闭 TLS、CORS、OIDC、fail-closed 限流或安全组。
- 不要把 Provider 失败转换成成功的 Mock 答案。
- COMMERCEFLOW_AI_FALLBACK_ENABLED=false 必须在 staging/production 保持关闭。
- 不要把 MySQL 3306、Redis 6379、Java 8080、Keycloak 8180 或本地前端端口暴露到公网。

Phase A - 资源盘点：
- 记录 ECS ID、地域、VPC、vSwitch、公网 IP、私网 IP、SSH 用户和密钥路径。
- 确认 wzl8.top 的 DNS 管理权和 ICP 状态。
- 确认 RDS、Redis、OIDC、DeepSeek Secret 是否存在。
- 缺少信息时，一次只向用户询问最少的一项。

Phase B - 本地基线：
- 执行 Java 测试。
- 执行 Admin 测试和生产构建。
- 执行 H5 测试和生产构建。
- 执行 Compose config 校验。
- 执行生产依赖审计。
- 执行 DeepSeek 合成 smoke，但不要把 API Key 写进命令行输出。
- 如果失败，先修复根因，再进入云端。

Phase C - 云资源和网络：
- 只有在用户确认费用和地域后创建资源。
- ECS 采用 SSH Key，不把 root 密码作为长期方案。
- 安全组只开 22（可信 IP）、80、443。
- RDS/Redis 使用私网地址和白名单。
- 记录所有资源 ID 和回滚操作。

Phase D - Secret 和配置：
- 使用服务器未提交 .env、阿里云 Secret 或同等安全存储。
- 复制 deploy/staging/.env.example 到服务器受保护路径。
- 替换全部 CHANGE_ME。
- OIDC issuer 必须是 HTTPS 且容器能访问 discovery/JWKS。
- 前端只允许公开 OIDC Client ID，不允许出现 Client Secret 或 AI Key。
- CORS 只写准确的 HTTPS origins。
- Redis 限流身份哈希 Secret 必须随机且独立。

Phase E - 部署：
- 先在 staging 子域名部署。
- 运行环境变量校验。
- 运行 docker compose config -q。
- 拉起 MySQL、Redis、Java API、Nginx/Caddy。
- 先从内部网络检查 readiness，再从公网检查 edge /health。
- 记录镜像版本、数据库迁移版本、部署时间和回滚版本。

Phase F - 验收：
必须输出每一项的 PASS、FAIL 或 BLOCKED 和证据：

1. staging 域名 DNS 已解析。
2. HTTPS 证书有效且能自动续期。
3. /health 对外可用；actuator/readiness 不对公网开放。
4. 未登录消费者请求返回 401。
5. 消费者 Token 只能访问自己的 /api/v1/me/**。
6. 消费者访问 /api/v1/operator/** 返回 403。
7. Operator Token 可以访问运营接口。
8. 真实 DeepSeek 返回 ANSWERED、Evidence 和 Trace。
9. Provider 401、429、5xx、超时、非法 JSON 时返回可识别失败，不生成假成功。
10. 下单幂等、库存不足回滚、订单快照和 Outbox 证据仍然成立。
11. Admin 浏览器流程通过。
12. Mobile/H5 浏览器流程通过。
13. 刷新页面后 OIDC 会话行为符合设计。
14. MySQL 备份可以恢复到临时实例。
15. Redis 不接受公网任意连接。
16. 服务器日志没有 API Key、Token、密码。

Phase G - 持续升级：
- 先做 P0 生产阻塞项：正式 OIDC、备份恢复、HTTPS、监控、回滚。
- 再做 P1：Outbox dispatcher、重试、死信、事件重放、AI 失败矩阵、评测集和成本指标。
- 每次升级必须先写目标和风险，只修改相关文件，运行相关测试，运行全量回归，做浏览器验收，更新 HANDOFF/TODO/证据，并输出回滚方式。
- 不要为了“看起来高级”添加没有真实业务闭环的支付、物流或推荐功能。

交付格式：
1. 当前状态：LOCAL_PASS、STAGING_PASS、PUBLIC_PASS 或 BLOCKED。
2. 已完成清单。
3. 未完成清单。
4. 真实命令和测试结果。
5. 云资源清单，但不要输出 Secret。
6. 域名、TLS、OIDC、Provider、数据库和 Redis 验收结果。
7. 风险和回滚方式。
8. 下一步只给用户一个最小可执行动作。

任何不能验证的内容必须写成 PENDING 或 BLOCKED，不能写成已完成。
~~~

---

## 6. Staging 完成门槛

只有下面全部满足，才能讨论正式域名：

- ECS、RDS、Redis 同地域/VPC 连接稳定。
- 80/443 可访问，22 不对全网开放。
- MySQL/Redis 不暴露公网。
- HTTPS 有效，证书能续期。
- OIDC issuer、audience、roles、PKCE 回调全部正确。
- 消费者和 Operator 权限边界通过。
- DeepSeek 真实成功和失败矩阵通过。
- API Key、Token、密码不进入日志和前端。
- 数据库备份可以实际恢复。
- Java、Admin、H5、Compose 和依赖审计通过。
- staging 浏览器完整流程通过。
- 有版本号、回滚命令和发布记录。

---

## 7. 后续升级优先级

### P0：生产阻塞项

1. 阿里云 staging 真实部署。
2. 正式 OIDC 和 HTTPS。
3. MySQL 备份恢复。
4. Redis 白名单和 fail-closed 限流。
5. 监控、告警和回滚。

### P1：项目含金量增强

1. Outbox dispatcher、重试、死信和人工重放。
2. DeepSeek 429/5xx/超时/非法 JSON 失败矩阵。
3. AI 评测集：Answer Accuracy、Evidence Coverage、Citation Validity、Latency、Cost。
4. Provider request id、AI trace id、order id 的统一可观测性。
5. Playwright staging 浏览器回归。

### P2：业务扩展

1. 支付接入。
2. 物流和售后。
3. 优惠券。
4. 用户评价。
5. 推荐和搜索排序。

P2 不应早于 P0。没有真实业务闭环时，不要为了简历堆假支付、假物流或假推荐。

---

## 8. 当前结论

~~~text
本地功能和测试：已通过
本地真实 DeepSeek：已通过
本地 OIDC/PKCE/API 权限：已通过
阿里云 staging 配置骨架：已准备
阿里云真实资源：待盘点/待确认
正式 OIDC：待配置
DNS/HTTPS/备份恢复：待 staging 验收
公网部署：未完成
~~~

下一个 AI 必须从资源盘点开始，不得直接购买、改 DNS 或宣布公网部署完成。
