package com.commerceflow.mall.operations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShowcaseRuntimePolicyTests {

    @Test
    void demoIdentityRequiresExplicitLocalOrDemoModeAndMatchingConfiguration() {
        ShowcaseRuntimeProperties properties = configuredDemo();

        assertTrue(properties.isDemoUserEnabled());
        assertTrue(properties.isDemoOperatorEnabled());
        assertTrue(properties.isLegacyApiAllowed());

        properties.setMode("LOCAL");
        assertTrue(properties.isDemoUserEnabled());
        assertTrue(properties.isDemoOperatorEnabled());
        assertTrue(properties.isLegacyApiAllowed());

        properties.setMode("TEST");
        assertFalse(properties.isDemoUserEnabled());
        assertFalse(properties.isDemoOperatorEnabled());
        assertFalse(properties.isLegacyApiAllowed());

        properties.setMode("STAGING");
        assertFalse(properties.isDemoUserEnabled());
        assertFalse(properties.isDemoOperatorEnabled());
        assertFalse(properties.isLegacyApiAllowed());

        properties.setMode("PRODUCTION");
        assertFalse(properties.isDemoUserEnabled());
        assertFalse(properties.isDemoOperatorEnabled());
        assertFalse(properties.isLegacyApiAllowed());
    }

    @Test
    void anUnknownModeFailsClosedEvenWhenLegacyFlagIsTrue() {
        ShowcaseRuntimeProperties properties = configuredDemo();
        properties.setMode("some-unrecognized-mode");

        assertFalse(properties.isLocalOrDemoMode());
        assertFalse(properties.isDemoUserEnabled());
        assertFalse(properties.isDemoOperatorEnabled());
        assertFalse(properties.isLegacyApiAllowed());
    }

    private ShowcaseRuntimeProperties configuredDemo() {
        ShowcaseRuntimeProperties properties = new ShowcaseRuntimeProperties();
        properties.setMode("DEMO");
        properties.setDataScope("LOCAL_SHOWCASE");
        properties.setAuthenticationMode("DEMO_USER");
        properties.setDemoUserId(1L);
        properties.setOperatorAuthenticationMode("DEMO_OPERATOR");
        properties.setDemoOperatorId(9001L);
        properties.setLegacyApiEnabled(true);
        return properties;
    }
}
