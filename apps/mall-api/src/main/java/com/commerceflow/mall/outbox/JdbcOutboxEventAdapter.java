package com.commerceflow.mall.outbox;

import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.outbox.application.port.out.OutboxEventPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MySQL/H2 outbox adapter. The insert runs in the order service transaction;
 * no event is marked as delivered by this adapter.
 */
@Component
public class JdbcOutboxEventAdapter implements OutboxEventPort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcOutboxEventAdapter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(OrderCreatedEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new CommerceException("OUTBOX_SERIALIZATION_FAILED", "Order event could not be serialized");
        }
        String eventId = "order-created:" + event.orderNo();
        jdbc.update(
                "INSERT INTO outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload_json,status,attempts) VALUES (?,?,?,?,?,?,?)",
                eventId,
                "ORDER",
                event.orderNo(),
                "ORDER_CREATED",
                payload,
                "PENDING",
                0);
    }
}
