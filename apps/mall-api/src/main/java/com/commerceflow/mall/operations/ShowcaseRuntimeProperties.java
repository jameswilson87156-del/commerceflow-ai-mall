package com.commerceflow.mall.operations;

import com.commerceflow.mall.ai.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("commerceflow.showcase")
public class ShowcaseRuntimeProperties {
    private String dataScope = "LOCAL_SHOWCASE";
    private String authenticationMode = "DEMO_USER";
    private String mobileRuntime = "H5_VERIFIED";
    private String nonH5Runtime = "COMPILE_ONLY";

    public String getDataScope() { return dataScope; }
    public void setDataScope(String value) { dataScope = value; }
    public String getAuthenticationMode() { return authenticationMode; }
    public void setAuthenticationMode(String value) { authenticationMode = value; }
    public String getMobileRuntime() { return mobileRuntime; }
    public void setMobileRuntime(String value) { mobileRuntime = value; }
    public String getNonH5Runtime() { return nonH5Runtime; }
    public void setNonH5Runtime(String value) { nonH5Runtime = value; }
}
