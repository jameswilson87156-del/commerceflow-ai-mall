package com.commerceflow.mall.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderExecutionEvidenceDto {
    private String orderNo;
    private Long userId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime createdAt;
    private String idempotencyKey;
    private String requestResult;
    private boolean firstCreation;
    private boolean idempotencyReplay;
    private List<OrderEvidenceItemDto> items = new ArrayList<>();
    private List<InventoryMovementEvidenceDto> inventoryMovements = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestResult() { return requestResult; }
    public void setRequestResult(String requestResult) { this.requestResult = requestResult; }
    public boolean isFirstCreation() { return firstCreation; }
    public void setFirstCreation(boolean firstCreation) { this.firstCreation = firstCreation; }
    public boolean isIdempotencyReplay() { return idempotencyReplay; }
    public void setIdempotencyReplay(boolean idempotencyReplay) { this.idempotencyReplay = idempotencyReplay; }
    public List<OrderEvidenceItemDto> getItems() { return items; }
    public void setItems(List<OrderEvidenceItemDto> items) { this.items = items; }
    public List<InventoryMovementEvidenceDto> getInventoryMovements() { return inventoryMovements; }
    public void setInventoryMovements(List<InventoryMovementEvidenceDto> inventoryMovements) { this.inventoryMovements = inventoryMovements; }
}
