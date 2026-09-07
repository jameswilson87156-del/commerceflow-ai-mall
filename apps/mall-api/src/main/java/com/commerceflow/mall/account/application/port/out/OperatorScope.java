package com.commerceflow.mall.account.application.port.out;

/**
 * Server-owned management actor boundary.
 *
 * <p>This is intentionally separate from {@link CurrentUserPort}: a customer
 * scope may only address that customer's resources, while an operator scope is
 * the prerequisite for cross-user operational reads.</p>
 */
public interface OperatorScope {
    Actor currentOperator();

    enum Role {
        OPERATOR,
        ADMIN
    }

    record Actor(long operatorId, String username, String displayName, Role role, String source) {
    }
}
