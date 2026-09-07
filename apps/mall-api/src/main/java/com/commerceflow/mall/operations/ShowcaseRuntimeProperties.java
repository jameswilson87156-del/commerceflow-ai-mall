package com.commerceflow.mall.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Explicit runtime boundary for the local Showcase and future deployed modes.
 *
 * <p>The safe defaults deliberately do not create a user or operator identity.
 * A demo identity exists only when a local/demo mode and the matching explicit
 * authentication configuration are both present.</p>
 */
@ConfigurationProperties("commerceflow.showcase")
public class ShowcaseRuntimeProperties {
    private String mode = "LOCAL";
    private String dataScope = "LOCAL";
    private String authenticationMode = "NONE";
    private long demoUserId = 0L;
    private String demoUsername = "demo@commerceflow.local";
    private String demoDisplayName = "Demo Shopper";
    private String operatorAuthenticationMode = "NONE";
    private long demoOperatorId = 0L;
    private String demoOperatorUsername = "operator@commerceflow.local";
    private String demoOperatorDisplayName = "Showcase Operator";
    private boolean legacyApiEnabled = false;
    private String mobileRuntime = "H5_VERIFIED";
    private String nonH5Runtime = "COMPILE_ONLY";

    public String getMode() { return mode; }
    public void setMode(String value) { mode = value; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String value) { dataScope = value; }
    public String getAuthenticationMode() { return authenticationMode; }
    public void setAuthenticationMode(String value) { authenticationMode = value; }
    public long getDemoUserId() { return demoUserId; }
    public void setDemoUserId(long value) { demoUserId = value; }
    public String getDemoUsername() { return demoUsername; }
    public void setDemoUsername(String value) { demoUsername = value; }
    public String getDemoDisplayName() { return demoDisplayName; }
    public void setDemoDisplayName(String value) { demoDisplayName = value; }
    public String getOperatorAuthenticationMode() { return operatorAuthenticationMode; }
    public void setOperatorAuthenticationMode(String value) { operatorAuthenticationMode = value; }
    public long getDemoOperatorId() { return demoOperatorId; }
    public void setDemoOperatorId(long value) { demoOperatorId = value; }
    public String getDemoOperatorUsername() { return demoOperatorUsername; }
    public void setDemoOperatorUsername(String value) { demoOperatorUsername = value; }
    public String getDemoOperatorDisplayName() { return demoOperatorDisplayName; }
    public void setDemoOperatorDisplayName(String value) { demoOperatorDisplayName = value; }
    public boolean isLegacyApiEnabled() { return legacyApiEnabled; }
    public void setLegacyApiEnabled(boolean value) { legacyApiEnabled = value; }
    public String getMobileRuntime() { return mobileRuntime; }
    public void setMobileRuntime(String value) { mobileRuntime = value; }
    public String getNonH5Runtime() { return nonH5Runtime; }
    public void setNonH5Runtime(String value) { nonH5Runtime = value; }

    public RuntimeMode runtimeMode() {
        return RuntimeMode.from(mode);
    }

    public boolean isLocalOrDemoMode() {
        return runtimeMode() == RuntimeMode.LOCAL || runtimeMode() == RuntimeMode.DEMO;
    }

    public boolean isDemoUserEnabled() {
        return isLocalOrDemoMode()
                && "DEMO_USER".equalsIgnoreCase(authenticationMode)
                && demoUserId > 0
                && nonBlank(demoUsername)
                && nonBlank(demoDisplayName);
    }

    public boolean isExternalConsumerAuthentication() {
        return "EXTERNAL".equalsIgnoreCase(authenticationMode)
                || "OIDC".equalsIgnoreCase(authenticationMode);
    }

    public boolean isExternalOperatorAuthentication() {
        return "EXTERNAL".equalsIgnoreCase(operatorAuthenticationMode)
                || "OIDC".equalsIgnoreCase(operatorAuthenticationMode);
    }

    public boolean isDemoOperatorEnabled() {
        return isLocalOrDemoMode()
                && "DEMO_OPERATOR".equalsIgnoreCase(operatorAuthenticationMode)
                && demoOperatorId > 0
                && nonBlank(demoOperatorUsername)
                && nonBlank(demoOperatorDisplayName);
    }

    /** Legacy query/body user-id routes are compatibility-only and never profile-global. */
    public boolean isLegacyApiAllowed() {
        return legacyApiEnabled && isLocalOrDemoMode();
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    public enum RuntimeMode {
        LOCAL,
        DEMO,
        TEST,
        STAGING,
        PRODUCTION,
        UNKNOWN;

        public static RuntimeMode from(String value) {
            if (value == null) return UNKNOWN;
            String normalized = value.trim().toUpperCase().replace('-', '_');
            if ("SHOWCASE".equals(normalized)) return DEMO;
            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return UNKNOWN;
            }
        }
    }
}
