# P4D Trace Timing Evidence

## Measurement method

All P4D Trace durations use `System.nanoTime()` and integer millisecond conversion. This is a monotonic elapsed-time measurement; `Instant` remains only a business timestamp and is not used for duration calculations.

`durationMs` and `latencyMs` are non-negative. A displayed `0 ms` means the measured phase completed in less than one whole millisecond after integer conversion; it is not forced to `1 ms`.

## Timing range

- Each `TraceStep.durationMs` measures only its named phase.
- `latencyMs` measures the full Java request path from the start of request validation through response and Evidence assembly.
- `latencyMs` intentionally excludes the subsequent `ai_trace` database insert, so persistence time is not presented as already-returned client latency.
- The sum of displayed whole-millisecond step values can be less than the total latency because each phase is rounded down independently and small handoff work exists between phase timers.

## Real local observations

| Local browser run | `BUSINESS_FACTS_LOADED` | `PROVIDER_COMPLETED` | `latencyMs` |
| --- | ---: | ---: | ---: |
| Final normal Mock screenshot | 7 ms | 4 ms, `COMPLETED` | 12 ms |
| Final stopped-Python fallback screenshot | 9 ms | 2 ms, `FALLBACK` | 12 ms |
| Stopped-Python shipping API check | not used as a screenshot value | 1 ms, `FALLBACK` | returned with 6 Trace steps |

The numbers above are local Showcase observations, not performance promises or benchmarks.
