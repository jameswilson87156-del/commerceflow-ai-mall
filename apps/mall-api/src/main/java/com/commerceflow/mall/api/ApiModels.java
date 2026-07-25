package com.commerceflow.mall.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public record LoginRequest(@NotBlank String username) {}
    public record LoginResponse(Long userId, String username, String displayName) {}
    public record CartItemRequest(@NotNull Long skuId, @Min(1) int quantity) {}
    public record OrderLineRequest(@NotNull Long skuId, @Min(1) int quantity) {}
    public record OrderRequest(@NotNull List<OrderLineRequest> items) {}
    public record ProductSummary(Long id, String name, String description, String categoryName, List<Sku> skus) {}
    public record Sku(Long id, String skuCode, String color, String size, BigDecimal salePrice, String currency, int availableStock) {}
    public record CartItem(Long id, Long skuId, String productName, String skuCode, String color, String size, BigDecimal unitPrice, int quantity, int availableStock) {}
    public record OrderSummary(String orderNo, BigDecimal totalAmount, String currency, String status, Instant createdAt, List<OrderItem> items) {}
    public record OrderItem(String productName, String skuCode, String attributes, BigDecimal unitPrice, int quantity) {}
    public record ProductChatRequest(@NotBlank String question, Long userId) {}
    public record AiAnswer(String traceId, String answer, String providerMode, String status, List<String> evidence) {}
}
