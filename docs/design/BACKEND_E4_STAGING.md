# CommerceFlow 后端 E4：Staging 部署资产

> 状态：本轮已完成可审查的 staging 部署骨架和安全检查脚本。日期：2026-09-03。当前没有执行 Docker 镜像发布、DNS 解析、证书签发、云资源创建或公网部署；真实身份和真实 Provider 仍需要运行前配置与单独验收。

> 2026-09-04 更新：OIDC/JWT Resource Server、消费者 external subject 映射、Operator 角色映射和 Admin/H5 PKCE 适配已落地；Java OpenAI-compatible adapter 已接入，且 `COMMERCEFLOW_AI_FALLBACK_ENABLED=false` 会真正 fail closed。真实 IdP token、有效 Provider 账户和公网服务器仍未在本工作区完成验收。

> 2026-09-04 安全加固：staging/production 启动守卫会拒绝 Demo 身份、legacy 路由、local fallback、非 `FAIL_CLOSED` 限流、占位数据库配置、缺失/不安全的限流 HMAC secret、缺失 Provider 配置和非 HTTPS CORS；Nginx 已不再公开映射 actuator，且补充基础安全响应头。

## 1. 目录和拓扑

```text
deploy/staging/docker-compose.yml
  ├─ mysql:8.4                 # 私有网络 + 持久卷 + healthcheck
  ├─ redis:8.0.2               # 私有网络 + 密码 + AOF 持久卷
  ├─ mall-api                  # Java 17/JRE，非 root，Flyway + readiness
  ├─ edge                      # Nginx 静态 Admin/H5 + /api 反代
  └─ caddy (profile: tls)      # 可选公网 TLS/ACME 终止
```

真实 Provider staging 由 `mall-api` 直接调用 OpenAI-compatible endpoint；`services/ai-service` 仍保留给本地 Showcase，不在当前真实 Provider Compose 拓扑中启动。

默认 edge 只绑定 `127.0.0.1:8088`，API、MySQL、Redis 和 FastAPI 没有宿主机端口。启用 `tls` profile 前，必须先让 Admin/H5 域名解析到机器并确认 80/443 未被其他服务占用。

## 2. 启动前检查

```powershell
Copy-Item .\deploy\staging\.env.example .\deploy\staging\.env
# 编辑 deploy/staging/.env，替换密码、域名、CORS 和可选 Provider 配置
powershell -NoProfile -File .\scripts\staging\validate-env.ps1
docker compose --env-file .\deploy\staging\.env -f .\deploy\staging\docker-compose.yml config
docker compose --env-file .\deploy\staging\.env -f .\deploy\staging\docker-compose.yml up -d --build
powershell -NoProfile -File .\scripts\staging\verify.ps1
```

需要真实 HTTPS 时，在 DNS、云防火墙、证书邮箱和回滚方案已经确认后运行：

```powershell
docker compose --env-file .\deploy\staging\.env -f .\deploy\staging\docker-compose.yml --profile tls up -d
```

## 3. 安全边界

- `mall-api` 使用 staging profile，关闭 query 参数 `userId` 兼容写路径；旧 `/api/cart`、`/api/orders`、`/api/ai` 和 Demo login 会返回 `LEGACY_ENDPOINT_DISABLED`。运营兼容路由同样受旧接口开关和 Operator policy 保护。
- `SHOWCASE_AUTHENTICATION_MODE` 不能是 `DEMO_USER`，Operator 也不能使用 Demo fixture；OIDC filter 负责验证 bearer JWT，应用层再由 `CurrentUserPort` / `OperatorScope` 派生资源范围。没有真实 token 时，`/api/v1/me` 返回 `401 UNAUTHENTICATED`，`/api/v1/operator` 返回 `401 OPERATOR_UNAUTHENTICATED`，因此这套 staging 骨架不会伪装成已登录系统。
- MySQL、Redis 不公开宿主机端口；Redis 开启密码和 AOF，订单事实仍只在 MySQL，Redis 不参与订单事务。
- Provider key 只能通过运行时环境注入，API 不把它放到浏览器、请求体、Trace、Outbox 或指标标签；真实 Provider 失败且 fallback disabled 时返回 `PROVIDER_ERROR`，不生成本地替代答案。
- edge 有请求体上限、上游连接/读取超时、基础 CSP / MIME / frame / referrer / permissions 响应头和最小化 `/health` 健康检查；`/api/actuator/**` 不再由 Nginx 对外映射，Java readiness 仅供容器网络和 Compose healthcheck 使用；Caddy 的 TLS profile 追加 HSTS，业务 API 仍由 edge 统一反代。

## 4. 恢复和回滚操作约束

当前文件只提供持久卷和 healthcheck，不声称已经有备份恢复演练。正式 staging 发布前还必须：

1. 由运维为 MySQL 建立定时备份并在隔离库恢复验证；
2. 保存镜像 tag、Flyway 版本和当前 `.env` 的 secret-manager 版本，不把 secret 文件复制到仓库；
3. 先执行 `docker compose config` 和 `scripts/staging/verify.ps1`，再切换 Caddy/TLS 流量；
4. 回滚应用镜像时不回滚已经成功应用的 Flyway 迁移，迁移回滚必须有独立的向前兼容方案；
5. 真实 Provider、IdP、域名和证书完成隔离验收后，才允许公开链接。

## 5. 当前退出条件

已完成：Dockerfile、私有网络、健康检查、Flyway 启动、非 root API 镜像、前端静态构建、Nginx 反代、可选 Caddy TLS、环境占位值拒绝和 catalog/readiness smoke 脚本；OIDC/JWT 认证边界、外部身份映射、Java Provider adapter 和真实 Provider fail-closed 配置也已实现。

未完成：真实 IdP token acceptance、备份恢复实测、公共 DNS/证书、真实 OpenAI/DeepSeek 成功调用、Outbox Worker、监控告警和公网发布。因此 E4 是“部署资产和连接点已准备”，不是“已经部署上线”。
