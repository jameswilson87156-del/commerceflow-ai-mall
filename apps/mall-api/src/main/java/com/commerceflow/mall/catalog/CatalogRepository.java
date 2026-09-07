package com.commerceflow.mall.catalog;

import com.commerceflow.mall.api.ApiModels;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    private final JdbcTemplate jdbc;
    public CatalogRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ApiModels.ProductSummary> findProducts() {
        return jdbc.query("SELECT p.id FROM product p WHERE p.status='ON_SALE' ORDER BY p.id", (rs, n) -> product(rs.getLong("id")));
    }
    public Optional<ApiModels.ProductSummary> findProduct(long id) {
        return jdbc.query("SELECT p.id FROM product p WHERE p.id=? AND p.status='ON_SALE'", (rs, n) -> product(rs.getLong("id")), id).stream().findFirst();
    }
    private ApiModels.ProductSummary product(long id) {
        var row = jdbc.queryForMap("SELECT p.id,p.product_code,p.name,p.description,p.status,p.cover_image_path,c.name category_name FROM product p JOIN product_category c ON c.id=p.category_id WHERE p.id=?", id);
        var skus = jdbc.query("SELECT s.id,s.sku_code,s.color,s.size,s.sale_price,s.currency,s.image_path,i.available_stock FROM product_sku s JOIN product p ON p.id=s.product_id JOIN inventory i ON i.sku_id=s.id WHERE s.product_id=? AND p.status='ON_SALE' AND s.status='ON_SALE' ORDER BY s.id", (rs, n) -> new ApiModels.Sku(rs.getLong("id"), rs.getString("sku_code"), rs.getString("color"), rs.getString("size"), rs.getBigDecimal("sale_price"), rs.getString("currency"), rs.getInt("available_stock"), rs.getString("image_path")), id);
        return new ApiModels.ProductSummary(((Number) row.get("id")).longValue(), (String) row.get("product_code"), (String) row.get("name"), (String) row.get("description"), (String) row.get("category_name"), (String) row.get("status"), (String) row.get("cover_image_path"), skus);
    }
    public ApiModels.Sku sku(long skuId) {
        return jdbc.queryForObject("SELECT s.id,s.sku_code,s.color,s.size,s.sale_price,s.currency,s.image_path,i.available_stock FROM product_sku s JOIN product p ON p.id=s.product_id JOIN inventory i ON i.sku_id=s.id WHERE s.id=? AND p.status='ON_SALE' AND s.status='ON_SALE'", (rs, n) -> new ApiModels.Sku(rs.getLong("id"), rs.getString("sku_code"), rs.getString("color"), rs.getString("size"), rs.getBigDecimal("sale_price"), rs.getString("currency"), rs.getInt("available_stock"), rs.getString("image_path")), skuId);
    }
    public String productName(long skuId) { return jdbc.queryForObject("SELECT p.name FROM product p JOIN product_sku s ON s.product_id=p.id WHERE s.id=?", String.class, skuId); }
    public long productId(long skuId) { return jdbc.queryForObject("SELECT product_id FROM product_sku WHERE id=?", Long.class, skuId); }

    public boolean productExists(long productId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*) > 0 FROM product WHERE id=?", Boolean.class, productId));
    }

    public boolean skuExists(long skuId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*) > 0 FROM product_sku WHERE id=?", Boolean.class, skuId));
    }

    /**
     * Customer service needs the actual selected SKU even when it is off sale.
     * Storefront listing queries intentionally continue to show only sellable SKUs.
     */
    public Optional<CustomerServiceSelection> findCustomerServiceSelection(long productId, long skuId) {
        String sql = """
            SELECT p.id product_id, p.product_code, p.name product_name, p.status product_status,
                   p.cover_image_path, s.id sku_id, s.sku_code, s.color, s.size, s.sale_price,
                   s.currency, s.status sku_status, COALESCE(s.image_path, p.cover_image_path) image_path,
                   i.available_stock
              FROM product p
              JOIN product_sku s ON s.product_id=p.id
              JOIN inventory i ON i.sku_id=s.id
             WHERE p.id=? AND s.id=?
            """;
        return jdbc.query(sql, (rs, n) -> new CustomerServiceSelection(
                rs.getLong("product_id"), rs.getString("product_code"), rs.getString("product_name"),
                rs.getString("product_status"), rs.getString("cover_image_path"), rs.getLong("sku_id"),
                rs.getString("sku_code"), rs.getString("color"), rs.getString("size"),
                rs.getBigDecimal("sale_price"), rs.getString("currency"), rs.getString("sku_status"),
                rs.getString("image_path"), rs.getInt("available_stock")), productId, skuId).stream().findFirst();
    }

    public record CustomerServiceSelection(
            long productId,
            String productCode,
            String productName,
            String productStatus,
            String productImagePath,
            long skuId,
            String skuCode,
            String color,
            String size,
            BigDecimal unitPrice,
            String currency,
            String skuStatus,
            String skuImagePath,
            int availableStock) {
    }
}
