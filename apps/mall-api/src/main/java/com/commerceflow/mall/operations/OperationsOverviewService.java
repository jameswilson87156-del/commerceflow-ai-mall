package com.commerceflow.mall.operations;

import com.commerceflow.mall.ai.AiProviderProperties;
import com.commerceflow.mall.ai.ratelimit.RateLimitProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsOverviewService {
    public static final int LOW_STOCK_THRESHOLD = 30;
    private static final int RECENT_ORDER_LIMIT = 5;
    private static final int LOW_STOCK_LIMIT = 8;
    private final OperationsOverviewMapper mapper;
    private final ShowcaseRuntimeProperties runtime;
    private final RateLimitProperties rateLimit;
    private final AiProviderProperties aiProvider;

    public OperationsOverviewService(OperationsOverviewMapper mapper, ShowcaseRuntimeProperties runtime, RateLimitProperties rateLimit,
                                     AiProviderProperties aiProvider) {
        this.mapper = mapper;
        this.runtime = runtime;
        this.rateLimit = rateLimit;
        this.aiProvider = aiProvider;
    }

    @Transactional(readOnly = true)
    public OperationsOverviewDto overview() {
        var summary = mapper.summary(LOW_STOCK_THRESHOLD);
        var ai = mapper.aiSummary();
        return new OperationsOverviewDto(
                new OperationsOverviewDto.Summary(summary.productCount(), summary.skuCount(), summary.onSaleProductCount(),
                        summary.availableStockTotal(), summary.lowStockSkuCount(), summary.createdOrderCount(),
                        zero(summary.createdOrderAmount()), summary.aiInteractionCount()),
                mapper.recentOrders(RECENT_ORDER_LIMIT).stream()
                        .map(row -> new OperationsOverviewDto.RecentOrder(row.orderNo(), row.status(), zero(row.totalAmount()), row.currency(), row.itemCount(), row.createdAt())).toList(),
                mapper.lowStockSkus(LOW_STOCK_THRESHOLD, LOW_STOCK_LIMIT).stream()
                        .map(row -> new OperationsOverviewDto.LowStockSku(row.skuId(), row.skuCode(), row.productName(), row.color(), row.size(), row.availableStock(), stockLevel(row.availableStock()))).toList(),
                new OperationsOverviewDto.AiSummary(ai.provider(), ai.providerMode(), ai.interactionCount(), ai.answeredCount(), ai.unsupportedCount(), ai.fallbackCount(), ai.providerErrorCount(), ai.latestInteractionAt()),
                new OperationsOverviewDto.RuntimeBoundary(runtime.getDataScope(), runtime.getAuthenticationMode(), aiProvider.runtimeMode(), "CREATED_ONLY",
                        "REDIS_LUA_FIXED_WINDOW", rateLimit.getLimit(), rateLimit.getWindowSeconds(), rateLimit.getFailurePolicy().name(),
                        runtime.getMobileRuntime(), runtime.getNonH5Runtime()),
                Instant.now());
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String stockLevel(int stock) { return stock == 0 ? "OUT_OF_STOCK" : stock < LOW_STOCK_THRESHOLD ? "LOW_STOCK" : "NORMAL"; }
}
