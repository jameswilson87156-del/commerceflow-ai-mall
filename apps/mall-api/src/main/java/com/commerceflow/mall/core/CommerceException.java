package com.commerceflow.mall.core;

public class CommerceException extends RuntimeException {
    private final String code;
    public CommerceException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
