package com.commerceflow.mall.core;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
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
