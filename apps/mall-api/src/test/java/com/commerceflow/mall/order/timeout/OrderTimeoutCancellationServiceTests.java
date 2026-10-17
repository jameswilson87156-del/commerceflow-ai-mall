package com.commerceflow.mall.order.timeout;

import com.commerceflow.mall.inventory.application.port.out.InventoryPort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.DueOrder;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.StoredItem;
import com.commerceflow.mall.outbox.application.port.out.OutboxEventPort;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderTimeoutCancellationServiceTests {

    @Test
    void cancelsDueOrderRestocksEverySkuAndEmitsOutboxEvent() {
        OrderWritePort orders = mock(OrderWritePort.class);
        InventoryPort inventory = mock(InventoryPort.class);
        OutboxEventPort outbox = mock(OutboxEventPort.class);
        when(orders.findTimeoutDue(anyInstant(), anyInt()))
                .thenReturn(List.of(new DueOrder("CF1", 1L)));
        when(orders.cancelIfCreated("CF1")).thenReturn(true);
        when(orders.storedItems("CF1")).thenReturn(List.of(
                new StoredItem(10001L, "SKU-A", 2),
                new StoredItem(10002L, "SKU-B", 1)));
        when(inventory.release(10001L, 2)).thenReturn(new InventoryPort.Reservation(10001L, 2, 5, 7));
        when(inventory.release(10002L, 1)).thenReturn(new InventoryPort.Reservation(10002L, 1, 9, 10));
        var service = new OrderTimeoutCancellationService(orders, inventory, outbox, 50);

        int cancelled = service.enforceDeadline(Instant.parse("2026-10-17T12:00:00Z"), 10);

        assertEquals(1, cancelled);
        verify(orders).cancelIfCreated("CF1");
        verify(inventory).release(10001L, 2);
        verify(inventory).release(10002L, 1);
        verify(orders).recordTimeoutRestock(eq("CF1"), eq(10001L), eq(2), eq(5), eq(7), eq("TIMEOUT:CF1"));
        verify(orders).recordTimeoutRestock(eq("CF1"), eq(10002L), eq(1), eq(9), eq(10), eq("TIMEOUT:CF1"));
        var eventCaptor = org.mockito.ArgumentCaptor.forClass(OutboxEventPort.OrderTimeoutCancelledEvent.class);
        verify(outbox).append(eventCaptor.capture());
        assertEquals("CF1", eventCaptor.getValue().orderNo());
        assertEquals(2, eventCaptor.getValue().items().size());
    }

    @Test
    void orderThatAdvancedConcurrentlyIsSkippedWithoutTouchingStockOrOutbox() {
        OrderWritePort orders = mock(OrderWritePort.class);
        InventoryPort inventory = mock(InventoryPort.class);
        OutboxEventPort outbox = mock(OutboxEventPort.class);
        when(orders.findTimeoutDue(anyInstant(), anyInt()))
                .thenReturn(List.of(new DueOrder("CF2", 1L)));
        when(orders.cancelIfCreated("CF2")).thenReturn(false);
        var service = new OrderTimeoutCancellationService(orders, inventory, outbox, 50);

        int cancelled = service.enforceDeadline(Instant.now(), 10);

        assertEquals(0, cancelled);
        verify(orders, never()).storedItems(anyString());
        verifyNoInteractions(inventory);
        verifyNoInteractions(outbox);
    }

    @Test
    void emptyScanCancelsNothing() {
        OrderWritePort orders = mock(OrderWritePort.class);
        when(orders.findTimeoutDue(anyInstant(), anyInt())).thenReturn(List.of());
        var service = new OrderTimeoutCancellationService(orders, mock(InventoryPort.class), mock(OutboxEventPort.class), 50);

        assertEquals(0, service.enforceDeadline(Instant.now(), 10));
        verify(orders, never()).cancelIfCreated(anyString());
    }

    private static Instant anyInstant() {
        return org.mockito.ArgumentMatchers.any(Instant.class);
    }
}
