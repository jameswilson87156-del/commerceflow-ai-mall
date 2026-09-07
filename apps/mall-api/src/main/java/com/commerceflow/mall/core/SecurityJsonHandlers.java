package com.commerceflow.mall.core;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Stable JSON responses for the resource-server filter, before MVC advice runs. */
public final class SecurityJsonHandlers {
    private SecurityJsonHandlers() {}

    public static AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> write(response, 401, "UNAUTHENTICATED", "A verified identity is required");
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(response, 403, "FORBIDDEN", "Insufficient permissions");
    }

    private static void write(HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
