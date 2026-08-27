package com.cmhk.business.module.admin.security;

/** 管理员令牌解析后的可信身份，包含角色及数据范围。 */
public record AdminPrincipal(
        Long userId,
        String username,
        String roleCode,
        String scopeType,
        Long scopeId) {

    /** 判断当前身份是否拥有系统管理员权限。 */
    public boolean isAdmin() {
        return "ADMIN".equals(roleCode);
    }

    /** 判断当前身份是否是可用的内部运营角色。 */
    public boolean isInternalOperator() {
        return isAdmin() || "OPERATOR".equals(roleCode);
    }
}
