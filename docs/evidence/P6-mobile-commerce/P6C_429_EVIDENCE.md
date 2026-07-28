# P6C 429 Evidence

After clearing the local Redis demonstration window, five real requests succeeded and the sixth request returned HTTP 429. The page showed limit `5`, remaining `0`, the preserved sixth question, one shared countdown, and disabled quick/send/retry controls. It did not render a sixth successful answer, Evidence, or Trace.

The top `Retry-After`, error-card countdown, and send-button countdown read the same `cooldownRemaining` state. Countdown does not go below zero and interval cleanup runs on zero and component unmount.

Screenshot: `screenshots/v2/07-mobile-ai-customer-service-429-real.png`.
