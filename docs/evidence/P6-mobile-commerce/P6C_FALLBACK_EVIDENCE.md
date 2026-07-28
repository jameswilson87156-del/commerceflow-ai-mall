# P6C Fallback Evidence

The local FastAPI process was stopped while MySQL, Redis, Java, and the H5 page remained available. The same stock question returned a Java fact fallback for gray L SKU 10004: stock `24`, price `129.00 CNY`, provider `java-fact-fallback / FALLBACK`.

The page retained the question and showed the returned fallback status. This fallback is a Java response based on current business facts, not a fabricated provider answer.

Screenshot: `screenshots/v2/07-mobile-ai-customer-service-fallback-real.png`.
