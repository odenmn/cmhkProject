package com.cmhk.business.module.admin.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** 集中维护P1管理端接口的最低角色权限。 */
@Component
public class AdminPermissionPolicy {

    /** 判断指定身份是否允许调用当前接口。 */
    public boolean isAllowed(AdminPrincipal principal, String method, String path) {
        if (principal == null || !principal.isInternalOperator()) {
            return false;
        }
        if (!isScopeAllowed(principal, method, path)) {
            return false;
        }
        if (isAdminOnly(method, path)) {
            return principal.isAdmin();
        }
        return true;
    }

    /** V1不开放渠道门户，CHANNEL范围仅允许已完成行级过滤的接口。 */
    private boolean isScopeAllowed(AdminPrincipal principal, String method, String path) {
        if (!"CHANNEL".equals(principal.scopeType())) {
            return true;
        }
        return path.startsWith("/api/admin/customers")
                || path.startsWith("/api/admin/orders")
                || path.startsWith("/api/admin/channels")
                || path.startsWith("/api/admin/secondary/records")
                || (HttpMethod.GET.matches(method) && path.startsWith("/api/admin/secondary/rules"));
    }

    private boolean isAdminOnly(String method, String path) {
        if (path.startsWith("/api/admin/users")) {
            return true;
        }
        if (path.startsWith("/api/admin/products") && !HttpMethod.GET.matches(method)) {
            return true;
        }
        if (path.startsWith("/api/admin/channels") && !HttpMethod.GET.matches(method)) {
            return true;
        }
        if (path.startsWith("/api/admin/secondary/channels") && !HttpMethod.GET.matches(method)) {
            return true;
        }
        if (path.startsWith("/api/admin/secondary/rules") && !HttpMethod.GET.matches(method)) {
            return true;
        }
        if (path.matches("/api/admin/secondary/records/\\d+/(adjust|confirm)")) {
            return true;
        }
        return false;
    }
}
