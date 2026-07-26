# P2 Product SKU Diff Report

## Compared Sources

- Target: `source/product-sku/target-page.png`
- Previous real page: `screenshots/v2/02-product-sku-real-final.png`
- Current components: `apps/admin-web/src/ProductSkuManagement.vue` and `style.css`
- Current data source: Java Product/SKU API backed by Flyway/MySQL

| Area | Target reference | Previous real page | Design-lock implementation decision |
| --- | --- | --- | --- |
| Sidebar and brand | 272px deep navy sidebar; CommerceFlow AI Mall lockup | 236px sidebar and compact brand | Use a 272px sidebar and target hierarchy while retaining only existing navigation entries. |
| Title and subtitle | Single title row, subtitle, search/filter controls directly below | Tall heading and a separate full-width filter bar | Use the copy specification and place the compact functional controls directly below the title to preserve the reference hierarchy. |
| Product list width | Narrow 426px master column | About 530px card column | Use a fixed master column close to the reference proportion. |
| Product cards | Image-led cards with summary metrics | Letter-token cards with minimal metrics | Use original local product images and real SKU count/minimum price/total stock. |
| Product image | 100px card image and large selected image | No product image field or rendered image | Add API-backed product and SKU image paths, then render formal frontend assets. |
| Detail layout | Large image at left; labeled details at right | Text identity block and compact definition list | Adopt the image-plus-facts composition using real name, code, category, status, and description. |
| SKU table | Wide eight-column data grid | Similar fields but no SKU image mapping or target density | Keep real technical fields, tune widths, and make row selection update the real SKU image. |
| Inventory facts | Bottom white card with icon and short facts | Teal callout | Preserve true inventory statements but adopt the target card placement and density. |
| Typography and chrome | 8-12px radius, subtle borders/shadows, strong navy headings | Similar palette but larger empty workspace | Tighten spacing, set closer card proportions, and reduce unused page space. |
| Target versus API data | Target has illustrative totals and two tote SKUs | API had two shirt SKUs and one tote SKU | Seed two products, four real shirt SKUs, and one tote SKU. No target number is hard-coded in Vue; totals are derived from API data. |

## Known Intentional Differences

The target's tote card depicts two SKUs, while the Showcase data retains one tote SKU because only one approved tote asset and one minimal variant are available. The target's aggregate stock values are replaced by real seeded API values. Product numeric IDs remain database IDs; `productCode` is added as a real API field to display the copy-spec identifiers.
