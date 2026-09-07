package com.commerceflow.mall.api;

import com.commerceflow.mall.account.AccountController;
import com.commerceflow.mall.ai.AiController;
import com.commerceflow.mall.catalog.CatalogController;
import com.commerceflow.mall.cart.CartController;
import com.commerceflow.mall.operations.OperationsOverviewController;
import com.commerceflow.mall.order.OrderController;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommerceApiContractTest {

    @Test
    void currentShowcaseRoutesRemainFrozen() {
        assertEquals(CommerceApiContract.AUTH_PATH, classPath(AccountController.class));
        assertRoute(AccountController.class, "login", PostMapping.class, CommerceApiContract.AUTH_DEMO_LOGIN_PATH);

        assertEquals(CommerceApiContract.PRODUCTS_PATH, classPath(CatalogController.class));
        assertRoute(CatalogController.class, "list", GetMapping.class, "");
        assertRoute(CatalogController.class, "detail", GetMapping.class, "/{id}");

        assertEquals(CommerceApiContract.CART_PATH, classPath(CartController.class));
        assertRoute(CartController.class, "get", GetMapping.class, "");
        assertRoute(CartController.class, "add", PostMapping.class, CommerceApiContract.CART_ITEMS_PATH);
        assertRoute(CartController.class, "update", PutMapping.class, CommerceApiContract.CART_ITEMS_PATH + "/{itemId}");
        assertRoute(CartController.class, "delete", DeleteMapping.class, CommerceApiContract.CART_ITEMS_PATH + "/{itemId}");

        assertEquals(CommerceApiContract.ORDERS_PATH, classPath(OrderController.class));
        assertRoute(OrderController.class, "submit", PostMapping.class, "");
        assertRoute(OrderController.class, "list", GetMapping.class, "");
        assertRoute(OrderController.class, "evidence", GetMapping.class, CommerceApiContract.ORDER_EXECUTION_EVIDENCE_PATH);
        assertRoute(OrderController.class, "detail", GetMapping.class, CommerceApiContract.ORDER_DETAIL_PATH);

        assertEquals(CommerceApiContract.AI_PATH, classPath(AiController.class));
        assertRoute(AiController.class, "ask", PostMapping.class, CommerceApiContract.AI_CUSTOMER_SERVICE_ASK_PATH);
        assertRoute(AiController.class, "deprecatedProductChat", PostMapping.class, CommerceApiContract.AI_LEGACY_PRODUCT_CHAT_PATH);

        assertEquals(CommerceApiContract.OPERATIONS_PATH, classPath(OperationsOverviewController.class));
        assertRoute(OperationsOverviewController.class, "overview", GetMapping.class, CommerceApiContract.OPERATIONS_OVERVIEW_PATH);
    }

    @Test
    void compatibilityFactsAndResponseShapesRemainFrozen() {
        assertEquals(CommerceApiContract.CURRENCY_CNY, ApiModels.SUPPORTED_CURRENCY);
        assertEquals(List.of(CommerceApiContract.ORDER_STATUS_CREATED),
                CommerceApiContract.SUPPORTED_ORDER_STATUSES.stream().sorted().toList());
        assertEquals(List.of("orderNo", "userId", "totalAmount", "currency", "status", "createdAt", "items"),
                recordComponentNames(ApiModels.OrderSummary.class));
        assertEquals(List.of("traceId", "clientRequestId", "answer", "answerStatus", "provider", "evidence", "businessFacts", "trace", "latencyMs", "fallbackUsed", "warning", "createdAt"),
                recordComponentNames(com.commerceflow.mall.ai.AiModels.CustomerServiceAnswer.class));
        assertTrue(Arrays.stream(com.commerceflow.mall.ai.AiModels.AnswerStatus.values())
                .anyMatch(status -> status.name().equals("FALLBACK_ANSWER")));
    }

    private String classPath(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertTrue(mapping != null, controller.getName() + " must declare a class route");
        assertEquals(1, mapping.value().length);
        return mapping.value()[0];
    }

    private void assertRoute(Class<?> controller, String methodName, Class<? extends Annotation> mappingType, String expectedPath) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Annotation mapping = method.getAnnotation(mappingType);
        assertTrue(mapping != null, controller.getName() + "#" + methodName + " must declare " + mappingType.getSimpleName());
        String[] actual = mappingPaths(mapping);
        assertArrayEquals(expectedPath.isEmpty() ? new String[0] : new String[]{expectedPath}, actual);
    }

    private String[] mappingPaths(Annotation annotation) {
        if (annotation instanceof GetMapping mapping) return mapping.value();
        if (annotation instanceof PostMapping mapping) return mapping.value();
        if (annotation instanceof PutMapping mapping) return mapping.value();
        if (annotation instanceof DeleteMapping mapping) return mapping.value();
        throw new IllegalArgumentException("Unsupported mapping annotation: " + annotation.annotationType());
    }

    private List<String> recordComponentNames(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(component -> component.getName()).toList();
    }
}
