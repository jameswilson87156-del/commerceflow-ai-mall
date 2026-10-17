package com.commerceflow.mall.order.timeout;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the fixed-delay deadline scan used by order timeout cancellation. */
@Configuration
@EnableScheduling
public class OrderTimeoutSchedulingConfig {
}
