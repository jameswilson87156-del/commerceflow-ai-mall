# P5C Capture Gap Analysis

## P5B issue

The CSS sidebar is fixed at `272px`. The P4 reference capture showed approximately 272 pixels, while the P5B PNG showed approximately 408 pixels despite reporting a 1920x1080 file. The issue was not the layout CSS.

The in-app browser ran on a host surface with `devicePixelRatio=1.5` and a default 1280x720 CSS viewport. A synthetic 1920x1080 CDP layout metric did not change that backing surface; its screenshot compositor preserved the effective 1.5x rendering. Therefore PNG metadata alone was not sufficient proof of a valid desktop capture.

## P5C capture environment

P5C uses a Chrome desktop viewport override rather than the affected in-app surface. Every final capture was measured before saving:

| Metric | Normal | 429 | FAIL_OPEN |
| --- | ---: | ---: | ---: |
| PNG pixels | 1920x1080 | 1920x1080 | 1920x1080 |
| `window.innerWidth` | 1920 | 1920 | 1920 |
| `window.innerHeight` | 1080 | 1080 | 1080 |
| `window.devicePixelRatio` | 1.00000003 | 1.00000003 | 1.00000003 |
| `visualViewport.scale` | 1 | 1 | 1 |
| `documentElement` zoom | 1 | 1 | 1 |
| Sidebar rect width | 272px | 272px | 272px |
| `clientWidth` / `scrollWidth` | 1920 / 1920 | 1920 / 1920 | 1920 / 1920 |

The final captures use `fullPage=false`, no CSS zoom, no transform scale, no crop, no stretch, no padding, and no reference-image embedding.
