package com.commerceflow.mall.account;

import com.commerceflow.mall.account.application.port.out.OperatorScope;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.operations.ShowcaseRuntimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Explicit local/demo operator adapter; it is not a production role provider. */
@Component
public class ShowcaseOperatorAdapter implements OperatorScope {
    private final ShowcaseRuntimeProperties runtime;
    private final ExternalIdentityResolver externalIdentityResolver;

    public ShowcaseOperatorAdapter(ShowcaseRuntimeProperties runtime) {
        this(runtime, null);
    }

    @Autowired
    public ShowcaseOperatorAdapter(ShowcaseRuntimeProperties runtime, ExternalIdentityResolver externalIdentityResolver) {
        this.runtime = runtime;
        this.externalIdentityResolver = externalIdentityResolver;
    }

    @Override
    public Actor currentOperator() {
        if (runtime.isExternalOperatorAuthentication()) {
            if (externalIdentityResolver == null) {
                throw new CommerceException("OPERATOR_UNAUTHENTICATED", "A trusted operator identity is required");
            }
            return externalIdentityResolver.currentOperator();
        }
        if (runtime.isDemoOperatorEnabled()) {
            return new Actor(runtime.getDemoOperatorId(), runtime.getDemoOperatorUsername(),
                    runtime.getDemoOperatorDisplayName(), Role.OPERATOR, "SHOWCASE_DEMO_OPERATOR");
        }
        if (runtime.isDemoUserEnabled()) {
            throw new CommerceException("OPERATOR_FORBIDDEN", "The current consumer identity is not an operator");
        }
        throw new CommerceException("OPERATOR_UNAUTHENTICATED", "A trusted operator identity is required");
    }
}
