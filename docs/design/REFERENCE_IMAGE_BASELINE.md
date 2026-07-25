# Reference Image Baseline

## Purpose

This document separates visual direction from runtime evidence. The generated image at `docs/design_refs/generated/01-dashboard-reference.png` is a design reference only. It is not a test result, API result, product screenshot, or proof that the pictured modules exist.

Evidence labels used in this baseline:

- `REAL_RUNTIME`: the current code can load or submit the data through a real local API.
- `REPLACE_WITH_REAL_DATA`: the visual pattern is useful, but every number, label, and row must come from the current API or an explicitly recorded demo fixture.
- `REMOVE`: the current project has no supporting capability and the feature must not be staged for a screenshot.
- `FUTURE_SCOPE`: a valid later product idea, but it is not part of the current showcase runtime.

## Current Reality Audit

| Area | Current implementation | Real entry point | Current boundary |
| --- | --- | --- | --- |
| Vue admin | One `App.vue` with client-side active-view state, not a route library | `GET /api/products`, `GET /api/orders?userId=1`, `POST /api/ai/product-chat` | Catalog, Orders, and AI Support have live data; Copy Desk and Trace Explorer are shells using the same chat call |
| UniApp | One page at `pages/index/index`; Mall and Bag are local states | `GET /api/products`, `POST /api/orders?userId=1`, `POST /api/ai/product-chat` | Product sheet and cart are local browser state; the Orders tab has no implemented order view; demo user is hard-coded |
| Java account | Demo login controller | `POST /api/auth/demo-login` | Demo authentication only; no sessions, roles, or user management |
| Java catalog | Product and SKU read repository/controller | `GET /api/products`, `GET /api/products/{productId}` | No product, category, or SKU write API |
| Java cart | Cart read/add repository/controller | `GET /api/cart?userId=1`, `POST /api/cart/items?userId=1` | The current UniApp flow keeps cart lines locally and does not call these cart endpoints |
| Java order | Transactional order service and read repository | `POST /api/orders?userId=1`, `GET /api/orders?userId=1`, `GET /api/orders/{orderNo}` | Status is `CREATED`; no payment, shipment, fulfillment, or cancellation state machine |
| Inventory | Guarded atomic decrement | `UPDATE inventory ... available_stock >= quantity` inside order transaction | `available_stock` only; no `locked_stock`, version field, or inventory read endpoint |
| AI customer support | Java builds facts, Python returns structured Mock answer, Java stores trace | `POST /api/ai/product-chat` and Python `POST /v1/product-answer` | Java has no trace history/detail read API; Python is not a fact source |
| AI copywriting | No dedicated implementation | None | Current `AI Copy Desk` shell incorrectly reuses the product-chat call; it is not a real copywriting feature |
| Evidence and Trace | `ai_trace` row is written on an AI request | Database table only | No API or UI that reads the trace row and renders evidence details |
| Flyway demo data | One user, two products, three SKUs, baseline stock | `V1__commerceflow_schema.sql`, `V2__demo_data.sql` | Orders and AI traces are created by runtime actions, not seeded as fixed facts |

## Eight Existing Runtime Captures

The eight files under `screenshots/` are real local captures and remain separate from the generated reference image. They were audited as artifacts, not regenerated in this round.

| File | Surface | Dimensions | Use in design work |
| --- | --- | --- | --- |
| `screenshots/admin-overview.jpg` | Vue admin | 1264 x 947 | Existing live overview evidence; do not use as a 1920 x 1080 claim |
| `screenshots/admin-catalog.jpg` | Vue admin | 1280 x 720 | Existing live catalog evidence |
| `screenshots/admin-orders.jpg` | Vue admin | 1280 x 720 | Existing live order evidence |
| `screenshots/admin-ai-support.jpg` | Vue admin | 1280 x 720 | Existing live Mock AI evidence |
| `screenshots/mobile-home.jpg` | UniApp H5 | 1280 x 720 | Existing H5 catalog evidence |
| `screenshots/mobile-product.jpg` | UniApp H5 | 1280 x 720 | Existing H5 product/SKU interaction evidence |
| `screenshots/mobile-bag.jpg` | UniApp H5 | 1280 x 720 | Existing H5 cart evidence |
| `screenshots/mobile-orders.jpg` | UniApp H5 | 1280 x 720 | Capture exists, but the current source has no real order tab implementation; verify the capture's claim before reuse |

## Verification Scope Already Available

- Java test suite: 4 tests passed, including transactional order and idempotency behavior.
- Python test suite: 2 tests passed; the existing Starlette/httpx deprecation warning remains recorded.
- Vue admin production build: passed.
- UniApp H5 build: passed.
- UniApp compiler build: passed.
- MySQL Docker healthcheck: passed.
- Mock Provider: default mode works without an API key; the optional OpenAI-compatible path is not treated as runtime evidence unless separately configured and tested.

## First Reference Image Review

The image presents a 1920 x 1080 desktop operations dashboard with a dark sidebar, four KPI cards, an order trend chart, a product/inventory table, an AI support panel, recent orders, a latest-activity feed, and a Trace evidence panel. It also shows navigation and state concepts for shipping, marketing, users, and settings.

| Reference region | Decision | Baseline rule |
| --- | --- | --- |
| CommerceFlow brand block | `KEEP` | Keep the brand zone and calm operations tone; use the actual showcase name and Mock Provider state |
| Dark sidebar and selected navigation treatment | `KEEP` | Keep the visual language, but limit navigation to implemented views: Overview, Catalog, Orders, AI Support |
| Global search, bell count, help, profile dropdown | `REMOVE` | No search index, notification service, help center, or profile/session UI exists |
| Today's orders | `REPLACE_WITH_REAL_DATA` | Use a clearly labelled demo order count from `GET /api/orders?userId=1`; do not imply a date-filtered daily metric |
| Pending shipment orders | `REMOVE` | No shipment or fulfillment state exists |
| Product total | `REPLACE_WITH_REAL_DATA` | Use the product count or SKU count computed from `GET /api/products`; label which one is shown |
| AI consultation completion rate | `REMOVE` | There is no aggregate endpoint, denominator, or completion metric |
| Order trend chart | `FUTURE_SCOPE` | Requires an aggregation endpoint and time semantics; do not draw a fabricated line from the two demo orders |
| Product and inventory table | `REPLACE_WITH_REAL_DATA` | Use product, category, SKU, price, currency, and `availableStock` from `GET /api/products` |
| Inventory status tags | `REPLACE_WITH_REAL_DATA` | A tag can be derived from `availableStock` only if the threshold is documented; do not show sales or replenishment data |
| AI customer-support workspace | `REPLACE_WITH_REAL_DATA` | Keep question, answer, Mock mode, status, trace ID, and `java.businessFacts` evidence; remove fake online/session indicators |
| Recent orders | `REPLACE_WITH_REAL_DATA` | Use `GET /api/orders?userId=1` and show order number, item snapshot, amount, currency, status, and created time |
| Latest activity feed | `REMOVE` | No activity/event API or fixture exists |
| Trace evidence card | `FUTURE_SCOPE` | The database row is written, but no trace read endpoint or detail screen exists |
| Marketing center | `REMOVE` | No marketing domain, campaign API, or data model exists |
| User management | `REMOVE` | Only one demo login record exists; no user CRUD or roles |
| Logistics and payment status | `REMOVE` | Explicitly outside Showcase V1 scope |
| Light background, dark text, restrained cards | `KEEP` | Use as the visual foundation for later real screenshots |

## Unified Visual Guidance

This is a design recommendation extracted from the reference image, not a claim that the current code already implements every token.

| Element | Recommendation |
| --- | --- |
| Brand zone | Use `CommerceFlow AI Mall` as the primary brand label, with a short `AI Mall / showcase` descriptor. Keep a compact mark and avoid invented company, customer, or revenue claims. |
| Sidebar | Use a deep navy-charcoal surface such as `#101C2C` or the existing dark shell. Selected navigation may use a restrained teal tint; show only real or explicitly design-only destinations. |
| Typography | Use one readable sans-serif for headings/body and a mono face for IDs, trace IDs, SKU codes, and status metadata. Use clear Chinese/English fallback stacks where localized copy is needed. |
| Type hierarchy | Desktop page title 28-32 px, section title 16-20 px, body 13-15 px, metadata 11-12 px. Do not use hero-scale type inside dense tables or evidence cards. |
| Card corners | Keep the current restrained 8 px baseline; a future reference may use 10-12 px only for a clearly framed workspace or bottom sheet. Avoid nested card-on-card decoration. |
| Page spacing | Use a 24-32 px content gutter, 16-24 px panel padding, and 12-16 px between repeated rows. Keep 8 px as the small spacing unit. |
| Primary interaction blue | Use a clear blue such as `#2F80ED` for selected controls, links, focus states, and primary data actions. Do not turn every label blue. |
| AI and success teal | Use `#21B7A6` for AI identity and `#2EAA72` for successful evidence/stock states. Keep error and fallback states separate from success teal. |
| Warning and error | Use amber for low-stock attention and red only for request failure, insufficient inventory, or idempotency conflict. The status must come from a real response or documented threshold. |
| Tables | Use a quiet header row, readable row height, left-aligned names, right-aligned money/stock, and a visible empty/loading/error state. Never add rows to make the table look full. |
| Status labels | Use compact labels for `ON_SALE`, `CREATED`, `COMPLETED`, `AI_FALLBACK`, and `MOCK`. Do not introduce `PAID`, `SHIPPED`, `DELIVERED`, or `REFUNDED`. |
| AI answer card | Keep the customer question, answer, provider mode, status, trace ID, and evidence tag visually grouped. Mark structured suggestions as suggestions, not guaranteed business truth. |
| Trace evidence card | Until a read API exists, keep this as a future pattern only. When implemented, show source, returned fields, risk, and timestamp with privacy-safe redaction. |
| Desktop frame | Primary reference frame is 1920 x 1080. Use a 240-280 px sidebar, a 32-48 px content gutter, and enough vertical room for real empty states. |
| Mobile frame | Use 390 x 844 as the primary reference and 375 x 812 as a compact check. Include top and bottom safe-area padding; keep bottom actions above the home indicator. |
| Responsive behavior | Collapse sidebar navigation into a compact header or tab bar only when the interaction remains real. Do not simply crop a desktop dashboard into a phone frame. |

## Working Rule

Design references may decide composition, hierarchy, and tone. The real API and current screenshots decide what may be shown as a completed feature. A design reference must never be cited in a README, interview answer, or test report as runtime evidence.
