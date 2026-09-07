package com.commerceflow.mall.account;

import com.commerceflow.mall.account.application.port.out.CurrentUserPort;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.operations.ShowcaseRuntimeProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** Current-user adapter for the explicit local Showcase authentication mode. */
@Component
public class ShowcaseCurrentUserAdapter implements CurrentUserPort {
    private final ShowcaseRuntimeProperties runtime;
    private final ExternalIdentityResolver externalIdentityResolver;

    public ShowcaseCurrentUserAdapter(ShowcaseRuntimeProperties runtime) {
        this(runtime, null);
    }

    @Autowired
    public ShowcaseCurrentUserAdapter(ShowcaseRuntimeProperties runtime, ExternalIdentityResolver externalIdentityResolver) {
        this.runtime = runtime;
        this.externalIdentityResolver = externalIdentityResolver;
    }

    @Override
    public UserScope currentUser() {
        if (runtime.isExternalConsumerAuthentication()) {
            if (externalIdentityResolver == null) {
                throw new CommerceException("UNAUTHENTICATED", "A trusted consumer identity is required");
            }
            return externalIdentityResolver.currentUser();
        }
        if (!runtime.isDemoUserEnabled()) {
            throw new CommerceException("UNAUTHENTICATED", "A trusted consumer identity is required");
        }
        return new UserScope(runtime.getDemoUserId(), runtime.getDemoUsername(), runtime.getDemoDisplayName(), "SHOWCASE_DEMO");
    }
}
