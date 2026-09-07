package com.commerceflow.mall.api;

import java.util.Set;

/**
 * Route contract for public catalog, consumer-scope and operator-scope APIs.
 *
 * <p>Unversioned routes are legacy compatibility routes. Versioned consumer
 * routes derive identity from {@code CurrentUserPort}; versioned operator
 * routes derive authorization from {@code OperatorScope}.</p>
 */
public final class CommerceApiContract {
    private CommerceApiContract() {
    }

    public static final String API_PREFIX = "/api";
    public static final String AUTH_PATH = API_PREFIX + "/auth";
    public static final String AUTH_DEMO_LOGIN_PATH = "/demo-login";
    public static final String PRODUCTS_PATH = API_PREFIX + "/products";
    public static final String CART_PATH = API_PREFIX + "/cart";
    public static final String CART_ITEMS_PATH = "/items";
    public static final String ORDERS_PATH = API_PREFIX + "/orders";
    public static final String ORDER_EXECUTION_EVIDENCE_PATH = "/{orderNo}/execution-evidence";
    public static final String ORDER_DETAIL_PATH = "/{orderNo}";
    public static final String AI_PATH = API_PREFIX + "/ai";
    public static final String AI_CUSTOMER_SERVICE_ASK_PATH = "/customer-service/ask";
    public static final String AI_LEGACY_PRODUCT_CHAT_PATH = "/product-chat";
    public static final String OPERATIONS_PATH = API_PREFIX + "/operations";
    public static final String OPERATIONS_OVERVIEW_PATH = "/overview";
    public static final String V1_PREFIX = API_PREFIX + "/v1";
    public static final String V1_ME_PATH = V1_PREFIX + "/me";
    public static final String V1_ME_CART_PATH = V1_ME_PATH + "/cart";
    public static final String V1_ME_ORDERS_PATH = V1_ME_PATH + "/orders";
    public static final String V1_ME_AI_PATH = V1_ME_PATH + "/ai";
    public static final String V1_ME_AI_CUSTOMER_SERVICE_ASK_PATH = "/customer-service/ask";

    public static final String V1_OPERATOR_PATH = V1_PREFIX + "/operator";
    public static final String V1_OPERATOR_ORDERS_PATH = V1_OPERATOR_PATH + "/orders";
    public static final String V1_OPERATOR_OPERATIONS_PATH = V1_OPERATOR_PATH + "/operations";
    public static final String V1_OPERATOR_AI_PATH = V1_OPERATOR_PATH + "/ai";
    public static final String V1_OPERATOR_AI_CUSTOMER_SERVICE_ASK_PATH = "/customer-service/ask";
    public static final String CURRENCY_CNY = "CNY";
    public static final String ORDER_STATUS_CREATED = "CREATED";

    public static final Set<String> SUPPORTED_ORDER_STATUSES = Set.of(ORDER_STATUS_CREATED);
}
