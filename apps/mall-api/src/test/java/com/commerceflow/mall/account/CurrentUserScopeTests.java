package com.commerceflow.mall.account;

import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.operations.ShowcaseRuntimeProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserScopeTests {

    @Test
    void showcaseAdapterReturnsConfiguredServerOwnedUser() {
        ShowcaseRuntimeProperties properties = new ShowcaseRuntimeProperties();
        properties.setMode("DEMO");
        properties.setAuthenticationMode("DEMO_USER");
        properties.setDemoUserId(7L);

        var scope = new ShowcaseCurrentUserAdapter(properties).currentUser();

        assertEquals(7L, scope.userId());
        assertEquals("demo@commerceflow.local", scope.username());
        assertEquals("SHOWCASE_DEMO", scope.source());
    }

    @Test
    void adapterRefusesToPretendDemoModeIsProductionAuthentication() {
        ShowcaseRuntimeProperties properties = new ShowcaseRuntimeProperties();
        properties.setAuthenticationMode("OIDC");

        CommerceException exception = assertThrows(
                CommerceException.class,
                () -> new ShowcaseCurrentUserAdapter(properties).currentUser());

        assertEquals("UNAUTHENTICATED", exception.code());
    }

    @Test
    void safeDefaultsDoNotCreateAnImplicitConsumerIdentity() {
        ShowcaseRuntimeProperties properties = new ShowcaseRuntimeProperties();

        CommerceException exception = assertThrows(
                CommerceException.class,
                () -> new ShowcaseCurrentUserAdapter(properties).currentUser());

        assertEquals("UNAUTHENTICATED", exception.code());
    }
}
