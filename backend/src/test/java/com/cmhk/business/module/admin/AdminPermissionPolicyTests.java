package com.cmhk.business.module.admin;

import com.cmhk.business.module.admin.security.AdminPermissionPolicy;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** P1角色与接口权限矩阵测试。 */
class AdminPermissionPolicyTests {

    private final AdminPermissionPolicy policy = new AdminPermissionPolicy();
    private final AdminPrincipal admin = new AdminPrincipal(1L, "admin", "ADMIN", "ALL", null);
    private final AdminPrincipal operator = new AdminPrincipal(2L, "operator", "OPERATOR", "CMHK", null);

    @Test
    void operatorCanHandleDailyBusinessButCannotManageUsers() {
        assertTrue(policy.isAllowed(operator, "GET", "/api/admin/customers"));
        assertTrue(policy.isAllowed(operator, "POST", "/api/admin/iccids/1/used"));
        assertFalse(policy.isAllowed(operator, "GET", "/api/admin/users"));
        assertTrue(policy.isAllowed(admin, "GET", "/api/admin/users"));
    }

    @Test
    void operatorCannotChangeRulesAdjustAmountsOrConfirmSettlement() {
        assertFalse(policy.isAllowed(operator, "POST", "/api/admin/secondary/rules"));
        assertFalse(policy.isAllowed(operator, "PUT", "/api/admin/secondary/rules/1"));
        assertFalse(policy.isAllowed(operator, "POST", "/api/admin/secondary/records/1/adjust"));
        assertFalse(policy.isAllowed(operator, "POST", "/api/admin/secondary/records/1/confirm"));
        assertTrue(policy.isAllowed(operator, "POST", "/api/admin/secondary/records/calculate"));
    }

    @Test
    void channelScopeOnlyAllowsEndpointsWithP1RowFiltering() {
        AdminPrincipal channelOperator = new AdminPrincipal(
                3L,
                "channel-operator",
                "OPERATOR",
                "CHANNEL",
                4L);
        assertTrue(policy.isAllowed(channelOperator, "GET", "/api/admin/customers"));
        assertTrue(policy.isAllowed(channelOperator, "GET", "/api/admin/orders"));
        assertTrue(policy.isAllowed(channelOperator, "GET", "/api/admin/secondary/records"));
        assertTrue(policy.isAllowed(channelOperator, "GET", "/api/admin/secondary/rules"));
        assertFalse(policy.isAllowed(channelOperator, "GET", "/api/admin/dashboard"));
        assertFalse(policy.isAllowed(channelOperator, "GET", "/api/admin/iccids"));
    }
}
