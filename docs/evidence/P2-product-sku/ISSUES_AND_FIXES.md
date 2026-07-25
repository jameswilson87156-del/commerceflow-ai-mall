# Issues and Fixes

## Local API Was Not Running

### Observation

The first direct local request to the mall API was refused.

### Root Cause

The local Java API process was not running. This was an environment state, not a Vue implementation failure.

### Resolution

Started the repository MySQL environment, then started `mall-api` on port `8081`. `GET /actuator/health` returned `UP`, after which the Vue page loaded the real product data.

## Detail Retry Target

### Observation

During implementation review, a failed request after selecting a new product could otherwise leave a retry action tied to the previously loaded detail object.

### Root Cause

The selected list identity and the successful detail payload were originally represented by one value.

### Resolution

Kept the selected product id separately from the detail payload so the selected card and retry path continue to refer to the product the user clicked.

## Development Dependency Audit Notice

### Observation

Adding the Vue test tooling produced npm development-dependency audit notices and a `glob@10.5.0` deprecation notice. `npm audit --omit=dev` reported zero production vulnerabilities.

### Resolution

No dependency replacement was made in this UI-scoped phase. The notices are not a build or test blocker; dependency remediation should be handled in a dedicated upgrade review rather than silently changing the toolchain here.

## P2.1 Screenshot Runtime and Favicon Request

### Observation

The Playwright package was available, but its default managed headless browser executable was absent. A first Chrome screenshot also reported one console error because the application had no favicon and the browser requested `/favicon.ico`.

### Resolution

Used the already-installed local Google Chrome executable through Playwright; no browser was downloaded or installed. Added an empty data favicon in `index.html`, then regenerated the candidate screenshot with `1920 x 1080`, `deviceScaleFactor: 1`, and `zh-CN`. The final capture reported no console errors.
