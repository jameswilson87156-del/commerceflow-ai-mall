package com.commerceflow.mall.account.application.policy;

import com.commerceflow.mall.account.application.port.out.OperatorScope;
import com.commerceflow.mall.core.CommerceException;
import org.springframework.stereotype.Component;

/** Single authorization entry point for every cross-user operator read. */
@Component
public class OperatorAuthorizationPolicy {
    private final OperatorScope operatorScope;

    public OperatorAuthorizationPolicy(OperatorScope operatorScope) {
        this.operatorScope = operatorScope;
    }

    public OperatorScope.Actor requireOperator() {
        OperatorScope.Actor actor = operatorScope.currentOperator();
        if (actor == null || actor.role() == null) {
            throw new CommerceException("OPERATOR_UNAUTHENTICATED", "A trusted operator identity is required");
        }
        if (actor.role() != OperatorScope.Role.OPERATOR && actor.role() != OperatorScope.Role.ADMIN) {
            throw new CommerceException("OPERATOR_FORBIDDEN", "The current identity cannot access operational data");
        }
        return actor;
    }
}
