package com.commerceflow.mall.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExternalAuthPropertiesTests {

    @Test
    void normalizesConfiguredOperatorRolesWithoutTrustingRolePrefixSpelling() {
        var properties = new ExternalAuthProperties();
        properties.setOperatorRoles(" ROLE_operator, admin, ,ROLE_support ");

        assertEquals(3, properties.operatorRoleSet().size());
        assertTrue(properties.operatorRoleSet().contains("OPERATOR"));
        assertTrue(properties.operatorRoleSet().contains("ADMIN"));
        assertTrue(properties.operatorRoleSet().contains("SUPPORT"));
    }
}
