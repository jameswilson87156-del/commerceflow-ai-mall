# Reference Image Issues

This register records visual-reference issues before implementation. It is not a defect list for the current runtime pages.

| Reference | Severity | Issue | Required correction |
| --- | --- | --- |
| 01 Dashboard | High | Marketing, user management, logistics, payment, notifications, and global search have no current domain/API. | Delete them from the implementation target. |
| 01 Dashboard | High | KPI values, daily trend, pending shipment, and AI completion rate have no approved source semantics. | Use only real product/order counts; defer aggregate KPIs and trend chart. |
| 01 Dashboard | Medium | The dashboard appears finished even though its source pages are incomplete. | Use as visual direction only; implement overview last and replace with a real V2 capture. |
| 02 Product/SKU | High | SKU counts and stock values do not match Flyway data. | Read Product, nested SKU, and `availableStock` from the local API. |
| 02 Product/SKU | High | Search, filter, create, edit, and bulk controls would be false affordances. | Omit until implemented with real behavior. |
| 02 Product/SKU | Medium | Product images and multiple SKU variants are not data-model fields. | Remove or label a later asset contract; do not treat generated images as product records. |
| 03 Order/Inventory | High | First creation, replay, and key conflict are shown like three independent orders. | Move them to a request-result panel; replay returns the original order and conflict returns 409 with no order row. |
| 03 Order/Inventory | High | Inventory values are static and lack an audit read API. | Show before / this deduction / after only after a real inventory-evidence response exists. |
| 03 Order/Inventory | Medium | English `CREATED` is treated as the main buyer-facing status. | Show Chinese status first; retain `CREATED` as the code status. |
| 04 AI Support | High | Provider, availability, trace ID, and facts are pictured as fixed values. | Render them from real runtime responses; no static availability claim. |
| 04 AI Support | Medium | Exception cards may be misread as actual incident history. | Label them as boundary examples; do not store or present them as observed failures. |
| 04 AI Support | High | Trace-history/detail is implied although only trace write exists. | Do not add history until a trace-read API is designed and implemented. |
| 07 Mobile Product | High | Black/M is selected while the bottom hint still asks for specification selection. | Synchronize selection and action state. |
| 07 Mobile Product | High | Zero-stock SKU must not remain purchasable. | Disable add/buy action and show an accessible unavailable state. |
| 07 Mobile Product | Medium | Product images do not have a current asset source; long variant list can overrun the viewport. | Use a common-image label or later image contract; make variant list collapsible. |
| 08 Mobile Order Flow | High | Demo user text is inconsistent and includes extra characters. | Use `U10001` consistently in the reference; define the runtime mapping from the existing demo user. |
| 08 Mobile Order Flow | High | Atomic update, 409, and idempotency mechanics are too prominent for a buyer journey. | Put technical evidence in a collapsible `Development demo information` section. |
| 08 Mobile Order Flow | High | Success could be misread as payment/shipment completion. | State `Order created` / `CREATED` only. |
| 08 Mobile Order Flow | Medium | `View order` has no real mobile detail route today. | Hide it until order detail is implemented and connected. |

## Cross-reference Rules

- Do not use an AI reference as a page background, an image inside a mock browser, or a claimed runtime screenshot.
- Do not hard-code a KPI or stock number solely to match a reference image.
- Do not introduce payment, logistics, marketing, user administration, AI copy, or trace history through design text alone.
- A new real screenshot must be generated after its page-specific build/test gate and must use Chinese UI plus real local API data.

