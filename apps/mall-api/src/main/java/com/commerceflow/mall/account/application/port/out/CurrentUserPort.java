package com.commerceflow.mall.account.application.port.out;

/**
 * Server-owned current-user boundary.
 *
 * <p>The current implementation is intentionally a Showcase adapter. The
 * versioned /me endpoints depend on this port so a future IdP can replace the
 * demo source without reintroducing client-controlled user IDs.</p>
 */
public interface CurrentUserPort {
    UserScope currentUser();

    record UserScope(long userId, String username, String displayName, String source) {
    }
}
