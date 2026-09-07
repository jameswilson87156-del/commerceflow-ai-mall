package com.commerceflow.mall.api;

import com.commerceflow.mall.account.CurrentUserController;
import com.commerceflow.mall.ai.OperatorAiController;
import com.commerceflow.mall.ai.ScopedAiController;
import com.commerceflow.mall.cart.ScopedCartController;
import com.commerceflow.mall.operations.OperatorOperationsOverviewController;
import com.commerceflow.mall.order.OperatorOrderController;
import com.commerceflow.mall.order.ScopedOrderController;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScopedApiContractTest {

    @Test
    void versionedMeRoutesHaveNoUserIdRequestParameter() {
        assertEquals(CommerceApiContract.V1_ME_PATH, classPath(CurrentUserController.class));
        assertMapping(CurrentUserController.class, "me", GetMapping.class, "");

        assertEquals(CommerceApiContract.V1_ME_CART_PATH, classPath(ScopedCartController.class));
        assertMapping(ScopedCartController.class, "get", GetMapping.class, "");
        assertMapping(ScopedCartController.class, "add", PostMapping.class, CommerceApiContract.CART_ITEMS_PATH);
        assertMapping(ScopedCartController.class, "update", PutMapping.class, CommerceApiContract.CART_ITEMS_PATH + "/{itemId}");
        assertMapping(ScopedCartController.class, "delete", DeleteMapping.class, CommerceApiContract.CART_ITEMS_PATH + "/{itemId}");

        assertEquals(CommerceApiContract.V1_ME_ORDERS_PATH, classPath(ScopedOrderController.class));
        assertMapping(ScopedOrderController.class, "submit", PostMapping.class, "");
        assertMapping(ScopedOrderController.class, "list", GetMapping.class, "");
        assertMapping(ScopedOrderController.class, "detail", GetMapping.class, "/{orderNo}");
        assertMapping(ScopedOrderController.class, "evidence", GetMapping.class, "/{orderNo}/execution-evidence");

        assertEquals(CommerceApiContract.V1_ME_AI_PATH, classPath(ScopedAiController.class));
        assertMapping(ScopedAiController.class, "ask", PostMapping.class, CommerceApiContract.V1_ME_AI_CUSTOMER_SERVICE_ASK_PATH);

        assertEquals(CommerceApiContract.V1_OPERATOR_ORDERS_PATH, classPath(OperatorOrderController.class));
        assertMapping(OperatorOrderController.class, "list", GetMapping.class, "");
        assertMapping(OperatorOrderController.class, "detail", GetMapping.class, "/{orderNo}");
        assertMapping(OperatorOrderController.class, "evidence", GetMapping.class, "/{orderNo}/execution-evidence");

        assertEquals(CommerceApiContract.V1_OPERATOR_OPERATIONS_PATH, classPath(OperatorOperationsOverviewController.class));
        assertMapping(OperatorOperationsOverviewController.class, "overview", GetMapping.class,
                CommerceApiContract.OPERATIONS_OVERVIEW_PATH);

        assertEquals(CommerceApiContract.V1_OPERATOR_AI_PATH, classPath(OperatorAiController.class));
        assertMapping(OperatorAiController.class, "ask", PostMapping.class,
                CommerceApiContract.V1_OPERATOR_AI_CUSTOMER_SERVICE_ASK_PATH);

        for (Class<?> controller : List.of(CurrentUserController.class, ScopedCartController.class, ScopedOrderController.class,
                ScopedAiController.class, OperatorOrderController.class, OperatorOperationsOverviewController.class,
                OperatorAiController.class)) {
            for (Method method : controller.getDeclaredMethods()) {
                for (var parameter : method.getParameters()) {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    if (requestParam != null) {
                        assertFalse("userId".equals(requestParam.name()) || "userId".equals(requestParam.value()));
                    }
                }
            }
        }
    }

    private String classPath(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals(1, mapping.value().length);
        return mapping.value()[0];
    }

    private void assertMapping(Class<?> controller, String methodName, Class<? extends Annotation> type, String expectedPath) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Annotation mapping = method.getAnnotation(type);
        assertNotNull(mapping);
        String[] actual;
        if (mapping instanceof GetMapping value) actual = value.value();
        else if (mapping instanceof PostMapping value) actual = value.value();
        else if (mapping instanceof PutMapping value) actual = value.value();
        else if (mapping instanceof DeleteMapping value) actual = value.value();
        else throw new IllegalArgumentException("Unsupported mapping: " + mapping.annotationType());
        assertEquals(expectedPath.isEmpty() ? 0 : 1, actual.length);
        if (!expectedPath.isEmpty()) assertEquals(expectedPath, actual[0]);
    }
}
