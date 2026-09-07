package com.commerceflow.mall.catalog;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CommerceApiContract.PRODUCTS_PATH)
public class CatalogController {
    private final CatalogRepository repository;
    public CatalogController(CatalogRepository repository) { this.repository = repository; }
    @GetMapping public List<ApiModels.ProductSummary> list() { return repository.findProducts(); }
    @GetMapping("/{id}") public ApiModels.ProductSummary detail(@PathVariable long id) { return repository.findProduct(id).orElseThrow(() -> new com.commerceflow.mall.core.CommerceException("PRODUCT_NOT_FOUND", "Product not found")); }
}
