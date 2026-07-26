# Open-Source License Matrix

**Research date:** 2026-07-26
**Scope:** P3.1 order-image research only. The repositories below were shallow-cloned outside CommerceFlow at `D:\workhome\commerceflow-reference-research`. No reference code, SQL, screenshots, logos, or product images were imported.

| Repository | Research commit | License found | Usage tier | Permitted research | Prohibited copying | Code / SQL adaptation | Image use | CommerceFlow adoption |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `macrozheng/mall` | `0504e86b1f1b6f1b8aa6a734d37a90fb67346be7` | Apache-2.0 | A | Table relationships, product/SKU separation, admin information architecture | Whole modules, seed SQL, screenshots, brand assets | Potentially permitted by license, but no reuse approved or performed | No | Naming and relationship ideas only; independently redesigned |
| `macrozheng/mall-admin-web` | `81fc17e5a19f452bd854b106a9d59fa1ed5c7eac` | Apache-2.0 | A | Order-detail information hierarchy and state grouping | Vue components, SVG/icons, screenshots, brand assets | Potentially permitted by license, but no reuse approved or performed | No | Layout observations only; independently redesigned |
| `crmeb/crmeb_java` | `ab6ca02882b9460c6579cc31ac898bb34640d3b9` | Apache-2.0 | A | Broad product/order organisation and multi-item order concepts | SQL, components, demo assets, admin UI | Potentially permitted by license, but no reuse approved or performed | No | Business-organisation observation only |
| `linlinjava/litemall` | `a1ef964a718b7277925b19ea26afe78ea3a1d325` | MIT | A | Compact goods/product/order modelling and order-item snapshot fields | SQL dumps, Mini Program pages, images, screenshots | Potentially permitted by license, but no reuse approved or performed | No | Snapshot-field comparison only |
| `newbee-ltd/newbee-mall-api` | `0879d5a1fac0c2a42c99eaf70d8b2f6e92aa05a4` | GPL-3.0 | B | Page/function partitioning and terminology only | Code, SQL, templates, static assets | No direct reuse | No | Design-and-idea comparison only |
| `newbee-ltd/newbee-mall-plus` | `a8c057d6145cf8c6f0dbc2dc05af8e1414f0bf2c` | GPL-3.0 | B | Server-rendered order-page organisation only | Code, SQL, templates, static assets | No direct reuse | No | Design-and-idea comparison only |

## Tier Rules

- **A - implementation reference:** Apache-2.0 and MIT repositories may inform relationships, field names, DTO shapes, mapper structure, and information architecture. They still may not be copied wholesale. Any future short-code reuse requires a prior user-approved entry naming the source file, license, range, retained notices, and an independent alternative.
- **B - design and idea reference only:** GPL-3.0 repositories may inform page structure, workflows, functional partitioning, and naming ideas. CommerceFlow must not copy their code, SQL, UI components, images, icons, templates, or resources.
- **C - prohibited:** content without a license, unclear commercial rights, personal demo data, trademarks, third-party product images, or unproven screenshots is not used.

## Evidence Locations

- `mall/document/sql/mall.sql`
- `mall-admin-web/src/views/oms/order/orderDetail.vue`
- `crmeb_java/crmeb/sql/Crmeb_v3.0.sql`
- `litemall/litemall-db/sql/litemall_table.sql`
- `newbee-mall-api/src/main/resources/newbee_mall_db_v2_schema.sql`
- `newbee-mall-plus/src/main/resources/newbee_mall_plus_schema.sql`

The external shallow clones are research material only and are deliberately outside the CommerceFlow repository and Git history.
