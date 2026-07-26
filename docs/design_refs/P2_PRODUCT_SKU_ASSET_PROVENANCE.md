# P2 Product SKU Asset Provenance

## Source Package

The approved source package is `docs/design_refs/source/product-sku/`.

- `target-page.png`: AI-generated visual reference only; never a runtime image or page background.
- `product-tshirt-white.png`, `product-tshirt-black.png`, `product-tshirt-gray.png`, `product-tshirt-navy.png`, and `product-tote-beige.png`: original AI-generated Showcase product assets.

## Runtime Mapping

| Showcase record | Runtime asset |
| --- | --- |
| T-shirt white SKU | `product-tshirt-white.png` |
| T-shirt black SKU | `product-tshirt-black.png` |
| T-shirt gray SKU | `product-tshirt-gray.png` |
| T-shirt navy SKU | `product-tshirt-navy.png` |
| Tote product and SKU | `product-tote-beige.png` |

The images are copied unchanged into the admin frontend's formal static asset directory. The Java API returns their runtime paths; the Vue page never reads image files directly from the documentation directory.

## Boundary

The assets have no third-party brand, logo, or watermark and are not third-party commercial material. They represent local Showcase seed data only, not real products, merchants, or transactions.
