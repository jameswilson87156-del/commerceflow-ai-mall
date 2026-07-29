package com.commerceflow.mall.operations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationsOverviewMapper {
    SummaryRow summary(@Param("lowStockThreshold") int lowStockThreshold);
    List<RecentOrderRow> recentOrders(@Param("limit") int limit);
    List<LowStockSkuRow> lowStockSkus(@Param("lowStockThreshold") int lowStockThreshold, @Param("limit") int limit);
    AiSummaryRow aiSummary();

    record SummaryRow(Long productCount, Long skuCount, Long onSaleProductCount, Long availableStockTotal,
                      Long lowStockSkuCount, Long createdOrderCount, BigDecimal createdOrderAmount, Long aiInteractionCount) { }
    record RecentOrderRow(String orderNo, String status, BigDecimal totalAmount, String currency, Long itemCount, Instant createdAt) { }
    record LowStockSkuRow(Long skuId, String skuCode, String productName, String color, String size, Integer availableStock) { }
    record AiSummaryRow(String provider, String providerMode, Long interactionCount, Long answeredCount,
                        Long unsupportedCount, Long fallbackCount, Long providerErrorCount, Instant latestInteractionAt) { }
}
