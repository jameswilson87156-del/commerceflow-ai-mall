package com.commerceflow.mall.ai.ratelimit;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class IdentityKeyResolver {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RateLimitProperties properties;

    public IdentityKeyResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    public String identityHash(long userId, String remoteAddress) {
        String normalizedAddress = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.getIdentityHashSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((userId + "|" + normalizedAddress).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.substring(0, 24);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive AI rate-limit identity", ex);
        }
    }
}
