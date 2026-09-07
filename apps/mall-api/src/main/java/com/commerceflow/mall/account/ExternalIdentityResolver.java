package com.commerceflow.mall.account;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.commerceflow.mall.account.application.port.out.CurrentUserPort;
import com.commerceflow.mall.account.application.port.out.OperatorScope;
import com.commerceflow.mall.core.CommerceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Converts a verified JWT into server-owned consumer or operator scope. */
@Component
public class ExternalIdentityResolver {
    private final ExternalAuthProperties properties;
    private final JdbcTemplate jdbc;

    public ExternalIdentityResolver(ExternalAuthProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    public CurrentUserPort.UserScope currentUser() {
        Jwt jwt = requireJwt("UNAUTHENTICATED", "A verified consumer identity is required");
        String subject = subject(jwt);
        String username = claimOrFallback(jwt, properties.getUsernameClaim(), "preferred_username", "email", "sub");
        String displayName = claimOrFallback(jwt, properties.getDisplayNameClaim(), "preferred_username", "email", "sub");
        UserRow mapped = findByExternalSubject(subject);
        if (mapped == null) mapped = findByTrustedUserIdClaim(jwt);
        if (mapped == null && properties.isAutoProvisionUsers()) mapped = provision(subject, username, displayName);
        if (mapped == null) {
            throw new CommerceException("USER_SCOPE_UNAVAILABLE", "The authenticated user is not mapped to an active account");
        }
        return new CurrentUserPort.UserScope(mapped.id(), mapped.username(), mapped.displayName(), "OIDC");
    }

    public OperatorScope.Actor currentOperator() {
        Jwt jwt = requireJwt("OPERATOR_UNAUTHENTICATED", "A verified operator identity is required");
        Set<String> roles = roles(jwt);
        if (roles.stream().noneMatch(properties.operatorRoleSet()::contains)) {
            throw new CommerceException("OPERATOR_FORBIDDEN", "The current identity cannot access operational data");
        }
        String subject = subject(jwt);
        String username = claimOrFallback(jwt, properties.getUsernameClaim(), "preferred_username", "email", "sub");
        String displayName = claimOrFallback(jwt, properties.getDisplayNameClaim(), "preferred_username", "email", "sub");
        OperatorScope.Role role = roles.contains("ADMIN") ? OperatorScope.Role.ADMIN : OperatorScope.Role.OPERATOR;
        long operatorId = numericClaim(jwt, properties.getOperatorIdClaim());
        if (operatorId <= 0) operatorId = stableId(subject);
        return new OperatorScope.Actor(operatorId, username, displayName, role, "OIDC");
    }

    public Jwt requireJwt(String code, String message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new CommerceException(code, message);
        }
        return token.getToken();
    }

    private String subject(Jwt jwt) {
        String value = jwt.getSubject();
        if (value == null || value.isBlank()) throw new CommerceException("UNAUTHENTICATED", "The external identity has no subject");
        return value.trim();
    }

    private UserRow findByExternalSubject(String subject) {
        String key = externalKey(subject);
        List<UserRow> rows = jdbc.query(
                "SELECT id, username, display_name FROM user_account WHERE external_subject=? AND status='ACTIVE'",
                (rs, rowNum) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("display_name")), key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private UserRow findByTrustedUserIdClaim(Jwt jwt) {
        long userId = numericClaim(jwt, properties.getUserIdClaim());
        if (userId <= 0) return null;
        List<UserRow> rows = jdbc.query(
                "SELECT id, username, display_name FROM user_account WHERE id=? AND status='ACTIVE'",
                (rs, rowNum) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("display_name")), userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private UserRow provision(String subject, String username, String displayName) {
        String safeUsername = limit(username.isBlank() ? subject : username, 80);
        String safeDisplayName = limit(displayName.isBlank() ? safeUsername : displayName, 120);
        try {
            jdbc.update("INSERT INTO user_account(username, display_name, status, external_subject) VALUES (?, ?, 'ACTIVE', ?)",
                    safeUsername, safeDisplayName, externalKey(subject));
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            // A concurrent first login may have created the mapping; read it below.
        }
        return findByExternalSubject(subject);
    }

    private String externalKey(String subject) {
        return limit((properties.getIssuerUri() == null ? "" : properties.getIssuerUri().trim()) + "|" + subject, 255);
    }

    private Set<String> roles(Jwt jwt) {
        Set<String> result = new LinkedHashSet<>();
        addValues(result, jwt.getClaims().get(properties.getRolesClaim()));
        addValues(result, jwt.getClaims().get("groups"));
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map) addValues(result, map.get("roles"));
        addValues(result, jwt.getClaims().get("scope"));
        return result;
    }

    private void addValues(Set<String> result, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> addValues(result, item));
        } else if (value instanceof String string) {
            for (String item : string.split("[ ,]")) {
                String normalized = item.trim().toUpperCase();
                if (normalized.startsWith("ROLE_")) normalized = normalized.substring(5);
                if (!normalized.isBlank()) result.add(normalized);
            }
        }
    }

    private String claimOrFallback(Jwt jwt, String primary, String... fallbacks) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        if (primary != null && !primary.isBlank()) names.add(primary);
        names.addAll(List.of(fallbacks));
        for (String name : names) {
            String value = jwt.getClaimAsString(name);
            if (value != null && !value.isBlank()) return limit(value.trim(), 120);
        }
        return "external-user";
    }

    private long numericClaim(Jwt jwt, String claimName) {
        if (claimName == null || claimName.isBlank()) return 0L;
        Object value = jwt.getClaims().get(claimName);
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? 0L : Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private long stableId(String subject) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
            return value == 0 ? 1 : value;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record UserRow(long id, String username, String displayName) {}
}
