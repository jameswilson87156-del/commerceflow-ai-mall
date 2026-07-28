package com.commerceflow.mall.core;

import java.time.Instant;
import java.util.Map;
import com.commerceflow.mall.ai.ratelimit.RateLimitExceededException;
import com.commerceflow.mall.ai.ratelimit.RateLimitHeaders;
import com.commerceflow.mall.ai.ratelimit.RateLimitUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<?> rateLimited(RateLimitExceededException ex) {
        var decision = ex.decision();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(RateLimitHeaders.rejected(decision))
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "code", "AI_RATE_LIMIT_EXCEEDED",
                        "message", ex.getMessage(),
                        "retryAfterSeconds", decision.retryAfterSeconds(),
                        "limit", decision.limit(),
                        "remaining", 0));
    }

    @ExceptionHandler(RateLimitUnavailableException.class)
    ResponseEntity<?> rateLimitUnavailable(RateLimitUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("timestamp", Instant.now(), "code", "AI_RATE_LIMIT_UNAVAILABLE", "message", ex.getMessage()));
    }

    @ExceptionHandler(CommerceException.class)
    ResponseEntity<?> commerce(CommerceException ex) {
        HttpStatus status = statusFor(ex.code());
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "code", ex.code(), "message", ex.getMessage()));
    }

    private HttpStatus statusFor(String code) {
        if (code.endsWith("_NOT_FOUND")) return HttpStatus.NOT_FOUND;
        if (code.contains("IN_PROGRESS") || code.contains("REUSED")) return HttpStatus.CONFLICT;
        if (code.equals("AI_SERVICE_UNAVAILABLE")) return HttpStatus.SERVICE_UNAVAILABLE;
        if (code.startsWith("AI_")) return HttpStatus.BAD_GATEWAY;
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "Request validation failed"));
    }
}
