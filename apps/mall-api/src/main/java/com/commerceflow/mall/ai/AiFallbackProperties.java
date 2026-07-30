package com.commerceflow.mall.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Java owns the decision to emit a fact-bound fallback answer. */
@Component
public class AiFallbackProperties {
    private final boolean enabled;

    public AiFallbackProperties(@Value("${commerceflow.ai.fallback-enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
