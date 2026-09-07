package com.commerceflow.mall.ai;

import com.commerceflow.mall.ai.application.port.out.CustomerServiceProvider;

/**
 * Compatibility name retained for existing tests and integrations.
 * New application code depends on {@link CustomerServiceProvider}.
 */
public interface CustomerServiceProviderClient extends CustomerServiceProvider {
}
