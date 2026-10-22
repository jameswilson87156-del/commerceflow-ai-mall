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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
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
        when(orders.cancelIfCreated("CF1")).thenReturn(true);
        when(orders.storedItems("CF1")).thenReturn(List.of(
                new StoredItem(10001L, "SKU-A", 2),
                new StoredItem(10002L, "SKU-B", 1)));
        when(inventory.release(10001L, 2)).thenReturn(new InventoryPort.Reservation(10001L, 2, 5, 7));
        when(inventory.release(10002L, 1)).thenReturn(new InventoryPort.Reservation(10002L, 1, 9, 10));
        var processor = new OrderTimeoutCancellationProcessor(orders, inventory, outbox);

        assertTrue(processor.cancel(new DueOrder("CF1", 1L)));

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
        when(orders.cancelIfCreated("CF2")).thenReturn(false);
        var processor = new OrderTimeoutCancellationProcessor(orders, inventory, outbox);

        assertFalse(processor.cancel(new DueOrder("CF2", 1L)));

        verify(orders, never()).storedItems(anyString());
        verifyNoInteractions(inventory);
        verifyNoInteractions(outbox);
    }

    @Test
    void oneOrderFailureDoesNotStopLaterCandidates() {
        OrderWritePort orders = mock(OrderWritePort.class);
        OrderTimeoutCancellationProcessor processor = mock(OrderTimeoutCancellationProcessor.class);
        DueOrder first = new DueOrder("CF3", 1L);
        DueOrder failed = new DueOrder("CF4", 2L);
        DueOrder last = new DueOrder("CF5", 3L);
        when(orders.findTimeoutDue(anyInstant(), anyInt())).thenReturn(List.of(first, failed, last));
        when(processor.cancel(first)).thenReturn(true);
        when(processor.cancel(failed)).thenThrow(new IllegalStateException("synthetic outbox failure"));
        when(processor.cancel(last)).thenReturn(true);
        var service = new OrderTimeoutCancellationService(orders, processor, 50);

        assertEquals(2, service.enforceDeadline(Instant.parse("2026-10-17T12:00:00Z"), 10));

        verify(processor).cancel(first);
        verify(processor).cancel(failed);
        verify(processor).cancel(last);
    }

    @Test
    void emptyScanCancelsNothing() {
        OrderWritePort orders = mock(OrderWritePort.class);
        OrderTimeoutCancellationProcessor processor = mock(OrderTimeoutCancellationProcessor.class);
        when(orders.findTimeoutDue(anyInstant(), anyInt())).thenReturn(List.of());
        var service = new OrderTimeoutCancellationService(orders, processor, 50);

        assertEquals(0, service.enforceDeadline(Instant.now(), 10));

        verifyNoInteractions(processor);
    }

    private static Instant anyInstant() {
        return org.mockito.ArgumentMatchers.any(Instant.class);
    }
}
