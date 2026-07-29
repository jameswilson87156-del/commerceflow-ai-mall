# P7 Implementation Plan

| Phase | Start branch | Scope | Verification and screenshot | Stop condition | Push/main permission |
| --- | --- | --- | --- | --- | --- |
| P7B | `feat/p7-showcase-release` from P6 final | Controlled local lifecycle; optional truthful read-only operations overview and minimum APIs. | Empty local runtime, health, API factuality, console, 1920x1080 overview only if implemented. | No metric can be backed by real data, or port/CORS contract is unresolved. | Push feature branch only; no main. |
| P7C | P7B accepted head | CI gaps, README, architecture/config docs, canonical screenshot links/checks. | CI equivalent locally; all canonical files/links/dimensions valid. | README or workflow claims exceed verified results. | Push candidate branch only. |
| P7D | P7C accepted head | History-aware security scan, empty-runtime release matrix, final PR/release preparation. | MySQL/Flyway V1-V8, Java/Python/admin/mobile tests/builds, Mock only, asset/screenshot and secret checks. | Any P0, secret, or unreproducible flow. | Only approved release owner may create PR/main/tag. |
| P7E | After release candidate is frozen | Portfolio wording, resume bullets, interview story, ownership and learning handoff. | Claim review against evidence and ownership gaps. | Any claim suggests commercial production or independent authorship without support. | Documentation branch; no release mutation needed. |

P7A has completed the planning artifact only. It does not authorize implementation outside a later approved phase.
