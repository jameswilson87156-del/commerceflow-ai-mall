package com.commerceflow.mall.order;

import java.math.BigDecimal;

public class OrderEvidenceItemDto {
    private Long itemId;
    private String productNameSnapshot;
    private String skuCodeSnapshot;
    private String colorSnapshot;
    private String sizeSnapshot;
    private String imagePathSnapshot;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public String getSkuCodeSnapshot() { return skuCodeSnapshot; }
    public void setSkuCodeSnapshot(String skuCodeSnapshot) { this.skuCodeSnapshot = skuCodeSnapshot; }
    public String getColorSnapshot() { return colorSnapshot; }
    public void setColorSnapshot(String colorSnapshot) { this.colorSnapshot = colorSnapshot; }
    public String getSizeSnapshot() { return sizeSnapshot; }
    public void setSizeSnapshot(String sizeSnapshot) { this.sizeSnapshot = sizeSnapshot; }
    public String getImagePathSnapshot() { return imagePathSnapshot; }
    public void setImagePathSnapshot(String imagePathSnapshot) { this.imagePathSnapshot = imagePathSnapshot; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
