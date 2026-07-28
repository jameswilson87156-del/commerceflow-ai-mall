# P6B.2 Layout Gap Analysis

Date: 2026-07-29

## Verified Cause

The original H5 layout mixed UniApp `view` elements that computed as inline boxes with CSS that assumed block or flex behavior. The result was not a data problem: normal-flow notice cards could overlap the first item, Flex children could stack under images, and generic panel styles could override a page-level Flex or Grid declaration. The product-detail Hero also inherited an oversized generic image rule.

## Changes

| Area | Verified problem | Final correction |
|---|---|---|
| Product list | Brand and refresh information competed for narrow width. | Added shrinkable brand content and compact text hierarchy; real two-product cards remain in normal flow. |
| Product detail | Hero text could be pushed beyond the viewport by the generic Hero image rule. | Scoped the hero image to 220px, used a vertical product card, and restored the screenshot to scroll position zero after selecting a SKU. |
| Cart | Notice card overlapped the first item; item copy stacked below its image; checkout bar could consume more than the intended height. | Added normal-flow `MobileNotice`, explicit `.cart-item.panel` Flex layout, stable three-column quantity control, and an 84px opaque checkout bar. |
| Confirm order | The notice and item cards inherited the same inline-child behavior. | Added normal-flow notice, explicit `.confirm-item.panel` Flex layout, and the shared 84px action bar. |
| Result | Inline panel rendering caused clipped white fragments at the left edge. | Explicit result-card block layout and two-column rows with safe monospace wrapping. |
| Order detail | The helper copy and item snapshot values could compress at narrow widths. | Stacked section heading, wrapped order number, explicit snapshot Flex layout, and two-column value grid. |

## Shared Rules

- `.mobile-page` is explicitly block-level and uses `--cf-bottom-bar-height: 84px` plus bottom spacing.
- `.bottom-action` is fixed, opaque, has a restrained shadow, supports safe area padding, and has a fixed 84px height.
- `MobileNotice` is normal document flow, `width: 100%`, `max-width: 100%`, `box-sizing: border-box`, and has no negative margin, transform, or absolute positioning.
- Stock labels use one policy module: zero is `缺货`, `1..9` is `库存偏低`, and `10+` is `库存正常`. This is display-only and does not alter Java inventory facts.

## Final Browser Bounds

At 390x844, the two cart cards are `20..370` horizontally and end at y=435 / y=624; the checkout bar begins at y=760. The two confirm cards end at y=371 / y=507; its action bar also begins at y=760. At 430x932, content uses `24..406` and the action bar begins at y=848. All six routes report `scrollWidth == clientWidth`, no core element outside the viewport, and decoded product images.
