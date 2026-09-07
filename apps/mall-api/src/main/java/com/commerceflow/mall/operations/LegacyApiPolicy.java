package com.commerceflow.mall.operations;

import com.commerceflow.mall.core.CommerceException;
import org.springframework.stereotype.Component;

/**
 * Keeps query/body user-id compatibility APIs inside explicit local/demo
 * runtime modes. A boolean flag cannot reopen them in staging/production.
 */
@Component
public class LegacyApiPolicy {
    private final ShowcaseRuntimeProperties runtime;

    public LegacyApiPolicy(ShowcaseRuntimeProperties runtime) {
        this.runtime = runtime;
    }

    public void requireEnabled() {
        if (!runtime.isLegacyApiAllowed()) {
            throw new CommerceException("LEGACY_ENDPOINT_DISABLED", "The legacy user-scoped API is disabled in this environment");
        }
    }
}
