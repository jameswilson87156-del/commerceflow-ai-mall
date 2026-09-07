package com.commerceflow.mall.account;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Runtime-only configuration for a verified external OIDC identity. */
@ConfigurationProperties("commerceflow.auth")
public class ExternalAuthProperties {
    private String mode = "SHOWCASE";
    private String issuerUri = "";
    private String audience = "";
    private String rolesClaim = "roles";
    private String principalClaim = "sub";
    private String usernameClaim = "email";
    private String displayNameClaim = "name";
    private String userIdClaim = "";
    private String operatorIdClaim = "";
    private String operatorRoles = "operator,admin";
    private boolean autoProvisionUsers;

    public String getMode() { return mode; }
    public void setMode(String value) { mode = value == null ? "SHOWCASE" : value.trim(); }
    public String getIssuerUri() { return issuerUri; }
    public void setIssuerUri(String value) { issuerUri = value == null ? "" : value.trim(); }
    public String getAudience() { return audience; }
    public void setAudience(String value) { audience = value == null ? "" : value.trim(); }
    public String getRolesClaim() { return rolesClaim; }
    public void setRolesClaim(String value) { rolesClaim = blankTo(value, "roles"); }
    public String getPrincipalClaim() { return principalClaim; }
    public void setPrincipalClaim(String value) { principalClaim = blankTo(value, "sub"); }
    public String getUsernameClaim() { return usernameClaim; }
    public void setUsernameClaim(String value) { usernameClaim = blankTo(value, "email"); }
    public String getDisplayNameClaim() { return displayNameClaim; }
    public void setDisplayNameClaim(String value) { displayNameClaim = blankTo(value, "name"); }
    public String getUserIdClaim() { return userIdClaim; }
    public void setUserIdClaim(String value) { userIdClaim = value == null ? "" : value.trim(); }
    public String getOperatorIdClaim() { return operatorIdClaim; }
    public void setOperatorIdClaim(String value) { operatorIdClaim = value == null ? "" : value.trim(); }
    public String getOperatorRoles() { return operatorRoles; }
    public void setOperatorRoles(String value) { operatorRoles = blankTo(value, "operator,admin"); }
    public boolean isAutoProvisionUsers() { return autoProvisionUsers; }
    public void setAutoProvisionUsers(boolean value) { autoProvisionUsers = value; }

    public boolean isOidc() { return "OIDC".equalsIgnoreCase(mode); }

    public Set<String> operatorRoleSet() {
        return Arrays.stream(operatorRoles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase().replace("ROLE_", ""))
                .collect(Collectors.toUnmodifiableSet());
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
