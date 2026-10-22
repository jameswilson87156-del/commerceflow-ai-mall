# CommerceFlow AI Mall：显式编码文本红队 V6

## 范围

数据集 [customer-service-redteam-v6-framed-encodings.json](customer-service-redteam-v6-framed-encodings.json) 包含 8 条合成样本：Base64、Base64URL、HEX 和显式 `ROT13:` 各一条攻击及一条正常价格查询控制。Base64URL 样本使用专属 `-` 和 `_` 字符，不只是普通 Base64 字符的子集。商品事实使用既有合成夹具；没有客户或生产数据。

## 实测结果

- `commerceflow-mock` / `MOCK`：攻击契约 4/4 通过；正常控制 4/4 通过；意图分类、状态、答案事实和场景契约均为 8/8；失败 0。
- 外部请求：0。当前逐样本记录在 [MOCK 结果 V2](customer-service-redteam-v6-framed-encodings-mock-results-v2.json)。首次运行结果保存在 [初始结果](customer-service-redteam-v6-framed-encodings-mock-results-initial.json)；V2 让 Base64URL 攻击和控制都实际使用 `-` 或 `_` 后重新运行。评测脚本原始输出：[v2 MOCK 命令日志](../../../../../deliverables/commerce-v6-framed-eval-v2-20261022.log)。
- 结果是确定性 MOCK 分类器与既有服务端事实渲染的离线回归，不是 DeepSeek 或其他语言模型的红队成绩。

## 实现与边界

策略匹配前仅解码显式标记的文本片段，要求 UTF-8 可打印文本，限制 token 长度、解码字节数及两轮解码。普通 Base64/HEX/ROT13 控制仍能落到 `PRICE` 并由服务端事实生成 ¥129.00；攻击文本先被分类为 `UNSUPPORTED`，不会改变商品价格或库存。没有引入依赖。

## 验证

| 命令 | 结果 |
| --- | --- |
| `py -3.12 -m pytest -q tests/test_real_provider.py tests/test_evaluate_provider.py -k 'framed_encodings or focused_redteam_reports or base64url_redteam_samples'`（目录：`services/ai-service`） | 退出码 0；12 项通过 |
| `py -3.12 -m pytest -q`（目录：`services/ai-service`） | 退出码 0；125 项通过 |
| `py -3.12 .\evaluate_provider.py --dataset .\evaluation\customer-service-redteam-v6-framed-encodings.json --output .\target\evaluation\customer-service-redteam-v6-framed-encodings-mock-v2-20261022.json` | 退出码 0；8/8 MOCK 契约通过，外部请求 0 |
| `mvn -o test`（目录：`apps/mall-api`） | 退出码 0；117 项，0 失败、0 错误、10 项跳过（Redis/MySQL 条件测试） |

原始日志：[定向编码与 URL-safe 测试](../../../../../deliverables/commerce-url-safe-encoding-targeted-20261022.log) · [Python 全量回归](../../../../../deliverables/commerce-full-pytest-framed-encodings-20261022.log) · [Maven 全量回归](../../../../../deliverables/commerce-full-maven-framed-encodings-20261022.log)。

## 面试讲法

“我先从固定的百分号/HTML 实体样本扩到显式 Base64、Base64URL、HEX 和 ROT13，并为每种攻击配同编码的正常价格控制。红测时 8 条都没有被旧分类器正确区分；加入有界解码后，MOCK 离线结果是 4/4 攻击按不支持处理、4/4 正常控制仍可询价，同时服务端只用既有商品事实生成答案。这个结果验证的是确定性策略和事实渲染，不是模型安全率。”

## 局限与参考

本集每种编码仅 1 条攻击和 1 条控制；没有覆盖无标记编码、组合编码、变体同义表达或模型生成行为。编码策略参考 [NVIDIA garak encoding probes](https://github.com/NVIDIA/garak/blob/main/garak/probes/encoding.py)，扫描技术与评分数据分开的思路参考 [Microsoft PyRIT scanner documentation](https://github.com/microsoft/PyRIT/blob/main/doc/scanner/0_scanner.md)。
