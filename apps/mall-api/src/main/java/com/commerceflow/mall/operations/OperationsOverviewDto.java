package com.commerceflow.mall.operations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OperationsOverviewDto(
        Summary summary,
        List<RecentOrder> recentOrders,
        List<LowStockSku> lowStockSkus,
        AiSummary aiSummary,
        RuntimeBoundary runtimeBoundary,
        Instant generatedAt) {
    public record Summary(long productCount, long skuCount, long onSaleProductCount, long availableStockTotal,
                          long lowStockSkuCount, long createdOrderCount, BigDecimal createdOrderAmount,
                          long aiInteractionCount) { }
    public record RecentOrder(String orderNo, String status, BigDecimal totalAmount, String currency, long itemCount, Instant createdAt) { }
    public record LowStockSku(long skuId, String skuCode, String productName, String color, String size,
                              int availableStock, String stockLevel) { }
    public record AiSummary(String provider, String providerMode, long interactionCount, long answeredCount,
                            long unsupportedCount, long fallbackCount, long providerErrorCount, Instant latestInteractionAt) { }
    public record RuntimeBoundary(String dataScope, String authenticationMode, String aiMode, String orderStatusScope,
                                  String rateLimitAlgorithm, int rateLimitLimit, int rateLimitWindowSeconds,
                                  String rateLimitFailurePolicy, String mobileRuntime, String nonH5Runtime) { }
}
