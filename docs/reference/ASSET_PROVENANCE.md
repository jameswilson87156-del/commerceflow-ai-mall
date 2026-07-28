# Asset Provenance

## Runtime Product Assets

The CommerceFlow showcase uses the five original local product assets prepared for the approved P2 design-source package. They are not downloaded from a reference repository.

| File | Source | AI generated | User provided / approved | Original to CommerceFlow | Used in runtime | Product or SKU | Public showcase use | Third-party project source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `product-tshirt-white.png` | CommerceFlow P2 design-source asset | Yes | Yes | Yes | Yes | White T-shirt SKU | Yes | No |
| `product-tshirt-black.png` | CommerceFlow P2 design-source asset | Yes | Yes | Yes | Yes | Black T-shirt SKU | Yes | No |
| `product-tshirt-gray.png` | CommerceFlow P2 design-source asset | Yes | Yes | Yes | Yes | Gray T-shirt SKU | Yes | No |
| `product-tshirt-navy.png` | CommerceFlow P2 design-source asset | Yes | Yes | Yes | Yes | Navy T-shirt SKU | Yes | No |
| `product-tote-beige.png` | CommerceFlow P2 design-source asset | Yes | Yes | Yes | Yes | Beige tote product/SKU | Yes | No |

## Source and runtime copies

- Approved source asset directory: `docs/design_refs/source/product-sku/`.
- Existing admin runtime source: `apps/admin-web/public/assets/products/`.
- Mobile runtime directory for this branch: `apps/mobile-app/src/static/assets/products/`.
- The mobile files are copied byte-for-byte from the approved local assets. No new or network image was introduced in P6B.1.
- The Java API returns relative paths such as `/assets/products/product-tshirt-gray.png`.
- In mobile development, the central `resolveImageUrl` helper maps those fields to `/src/static/assets/products/...`; in a built UniApp H5 package the same fields resolve to `/static/assets/products/...`.
- The mobile pages never read from `docs/` at runtime and do not request third-party image URLs. The earlier P6B `apps/mobile-app/public/assets/products/` copy is retained in history but is not the runtime source on this hardening branch.

## Mapping

| Product or SKU | Runtime asset |
| --- | --- |
| T-shirt / white SKU | `product-tshirt-white.png` |
| T-shirt / black SKU | `product-tshirt-black.png` |
| T-shirt / gray SKU | `product-tshirt-gray.png` |
| T-shirt / navy SKU | `product-tshirt-navy.png` |
| Tote bag SKU | `product-tote-beige.png` |

The mapping is carried by the real Product, SKU, CartItem, and OrderItem snapshot fields returned by the Java API. It is not a SKU-specific hardcoded map in page components.

## Design-source assets and rules

The same five product files are retained in `docs/design_refs/source/product-sku/` as approved source assets. `target-page.png` is an AI-generated P2 visual reference only, is not a runtime asset, and is not a real screenshot. `copy-spec.md` is approved Chinese copy guidance.

- Reference-project images, screenshots, brand marks, and icons remain research-only and are not copied into mobile runtime assets.
- Product images are used for local CommerceFlow product lists, product details, SKU selection, cart, and order snapshots.
- These assets do not represent a real merchant, product catalog, or transaction.
- The target design references are never used as a page background or runtime image source.
