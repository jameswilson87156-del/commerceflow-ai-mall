package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.catalog.CatalogRepository;
import com.commerceflow.mall.inventory.application.port.out.InventoryPort;
import com.commerceflow.mall.order.application.port.out.IdempotencyStore;
import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import com.commerceflow.mall.outbox.application.port.out.OutboxEventPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderReliabilityPortTest {

    @Test
    void orderApplicationDependsOnPortsInsteadOfJdbcRepositoryTypes() {
        var fieldTypes = Arrays.stream(OrderService.class.getDeclaredFields())
                .map(field -> field.getType().getName())
                .toList();

        assertTrue(fieldTypes.contains(OrderWritePort.class.getName()));
        assertTrue(fieldTypes.contains(IdempotencyStore.class.getName()));
        assertTrue(fieldTypes.contains(InventoryPort.class.getName()));
        assertTrue(fieldTypes.contains(OutboxEventPort.class.getName()));
        assertFalse(fieldTypes.contains(OrderRepository.class.getName()));
    }

    @Test
    void orderWritePortKeepsTheTransactionFactsNeededForEvidence() throws NoSuchMethodException {
        assertTrue(Arrays.stream(OrderWritePort.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("recordInventoryMovement")));
        assertTrue(Arrays.stream(OrderWritePort.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("clearCart")));
        assertTrue(OrderWritePort.OrderLine.class.getRecordComponents().length >= 12);
    }

    @Test
    void multiSkuReservationUsesAscendingSkuOrderEvenWhenRequestOrderIsReversed() {
        var writes = mock(OrderWritePort.class);
        var idempotency = mock(IdempotencyStore.class);
        var inventory = mock(InventoryPort.class);
        var catalog = mock(CatalogRepository.class);
        var outbox = mock(OutboxEventPort.class);
        var reservedSkuIds = new ArrayList<Long>();

        when(writes.userExists(1L)).thenReturn(true);
        when(idempotency.find(anyLong(), anyString())).thenReturn(Optional.empty());
        when(catalog.sku(10003L)).thenReturn(sku(10003L, "TOTE-BEIGE-ONE", "199.00"));
        when(catalog.sku(10004L)).thenReturn(sku(10004L, "T-SHIRT-GRAY-L", "129.00"));
        when(catalog.productId(10003L)).thenReturn(102L);
        when(catalog.productId(10004L)).thenReturn(101L);
        when(catalog.productName(10003L)).thenReturn("Tote");
        when(catalog.productName(10004L)).thenReturn("Shirt");
        when(inventory.reserve(anyLong(), anyInt())).thenAnswer(invocation -> {
            long skuId = invocation.getArgument(0, Long.class);
            reservedSkuIds.add(skuId);
            return new InventoryPort.Reservation(skuId, 1, 10, 9);
        });
        when(writes.insertOrder(org.mockito.ArgumentMatchers.any(OrderWritePort.OrderDraft.class))).thenReturn(1L);
        when(writes.find(anyString())).thenReturn(Optional.of(new ApiModels.OrderSummary(
                "CF-test", 1L, new BigDecimal("328.00"), "CNY", "CREATED", Instant.now(), List.of())));

        var service = new OrderService(writes, idempotency, inventory, catalog, outbox);
        service.submit(1L, "e2-unit-lock-order", new ApiModels.OrderRequest(List.of(
                new ApiModels.OrderLineRequest(10004L, 1),
                new ApiModels.OrderLineRequest(10003L, 1))));

        assertEquals(List.of(10003L, 10004L), reservedSkuIds);
    }

    private ApiModels.Sku sku(long skuId, String skuCode, String price) {
        return new ApiModels.Sku(skuId, skuCode, "颜色", "尺码", new BigDecimal(price), "CNY", 10, "/image.png");
    }
}
