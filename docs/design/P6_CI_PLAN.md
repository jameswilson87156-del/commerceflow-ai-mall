# P6 CI 评估计划

本轮不修改 CI，只定义下一阶段应审查的门槛。

| 检查项 | 当前 CI | P6 目标 |
| --- | --- | --- |
| 移动单元/组件测试 | 未配置 | P6B 增加可重复的 `npm test` 后再加入 |
| H5 构建 | 已有 `npm run build` | 保留并上传日志/产物摘要 |
| UniApp 构建 | 已有 `npm run build:uni` | 保留；不能宣称真机通过 |
| Java/MySQL | CI 有真实 MySQL 服务 | 移动契约变更后继续回归 Java 测试 |
| Redis/Python | P5/P4 已有对应 job/服务 | P6C 只在 AI 入口改动时回归，不在 P6B 引入新服务 |
| Java-mobile contract | 无专门检查 | 用 OpenAPI/接口样例和前端类型校验防止字段漂移 |
| API drift | 无 | 未来可加入 smoke script；失败时阻止截图型结论 |
| 真机/小程序 | 无 | 保持人工验收，不把 CI build 当设备证据 |

CI 不应把 `.env.local`、真实 API Key、数据库数据、node_modules、dist 或 target 打包进 artifact。

