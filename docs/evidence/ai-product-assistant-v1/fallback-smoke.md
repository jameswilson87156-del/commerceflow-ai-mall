# Local failure fallback smoke

A child-process-only project override selected a deliberately incomplete real-provider configuration. It fails before a network transport can be used. No shared environment variable was changed and no real credential was used.

## fallback enabled

- HTTP result: 200
- Provider mode: `FALLBACK`
- `fallbackUsed`: true
- Answer status: `FALLBACK_ANSWER`
- Java Evidence and Trace: present
- Remote provider call: not used

## fallback disabled

- HTTP result: 503
- Error code: `AI_SERVICE_UNAVAILABLE`
- Local answer: absent
- Fallback answer flag: absent
- Remote provider call: not used

This verifies that fallback ownership is in Java and that disabled fallback does not present a provider failure as success.