package com.commerceflow.mall.order.application.port.out;

import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;

/** Durable lookup boundary for a user's order idempotency key. */
@NoRepositoryBean
public interface IdempotencyStore {
    Optional<IdempotencyRecord> find(long userId, String idempotencyKey);

    record IdempotencyRecord(String orderNo, String requestFingerprint) {
    }
}
