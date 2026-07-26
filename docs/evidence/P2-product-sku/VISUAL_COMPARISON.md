# P2 Product SKU Visual Comparison

## Sources

- Target reference: `docs/design_refs/source/product-sku/target-page.png`
- Previous real page: `screenshots/v2/02-product-sku-real-final.png`
- New real page: `screenshots/v2/02-product-sku-design-lock-real.png`

The target is an AI-generated design reference only. The new page is a real local Vue runtime using Java API responses and local MySQL/Flyway Showcase data.

| Area | Target | Previous real page | New design-lock page | Result |
| --- | --- | --- | --- | --- |
| Page skeleton | Sidebar, title/filter band, master list, detail stack | Wider sparse master/detail arrangement | 272px sidebar, title/filter band, 426px list, detail stack | MATCHED |
| Card proportions | Two image-led narrow cards | Text-led cards without image fields | Two 174px image-led cards with real derived summaries | MATCHED |
| Product images | Large card and detail images | No product images | Original local assets from API paths in cards, detail, and SKU rows | MATCHED |
| Product cards | Two illustrative products and target totals | Two products with limited summaries | Two API products; count, price, and inventory totals are derived from returned SKUs | PARTIALLY_MATCHED |
| Detail region | Image, product code, category, status, description | Compact text details | Image plus real product code, status, category, description, and selected SKU facts | MATCHED |
| SKU table | Eight dense columns and stock tags | Table without SKU image mapping | Eight real columns, SKU thumbnails, selected-row behavior, and real tags | MATCHED |
| Inventory explanation | Short bottom fact card | Larger teal callout | Compact bottom fact card connected directly to the SKU table | MATCHED |
| Type, color, chrome | Navy shell, blue selection, teal/amber/red stock, restrained panels | Similar colors with more empty space | Same role colors, tighter density, 6-8px corners, subtle borders/shadows | MATCHED |
| Whitespace | Dense 1920px desktop composition | Large unused lower and left areas | Master and detail areas sized to the reference without decorative filler | MATCHED |
| Data truthfulness | Illustrative IDs, totals, and two tote SKUs | Real but limited prior seed | API/MySQL values only: 4 T-shirt SKUs, 1 tote SKU, actual seeded prices/stocks | PARTIALLY_MATCHED |

## Intentional Non-Matches

- The target depicts two tote SKUs; the approved local Showcase data intentionally retains one tote SKU because only one minimal tote variant and one approved tote asset are available.
- Target stock totals and some illustrative values are not copied. The page derives its figures from local Flyway data and exposes the real values returned by Java.
- The new screenshot selects the gray T-shirt SKU to demonstrate the SKU-to-image mapping. The target's selected SKU is a visual reference, not a required business state.

## Boundary

The page remains read-only. It has no create, edit, delete, bulk, supplier, sales, restock, payment, logistics, or AI write capability. No similarity percentage is claimed.
