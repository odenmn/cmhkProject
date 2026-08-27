package com.cmhk.business.module.admin.service;

import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.security.AdminPrincipal;

import java.util.List;
import java.util.Optional;

/** 管理后台用户认证与维护服务。 */
public interface AdminUserService {

    AdminUser authenticate(String username, String password);

    Optional<AdminPrincipal> findActivePrincipal(Long userId);

    List<AdminUser> list(AdminPrincipal principal);

    AdminUser create(AdminUser input, String rawPassword, AdminPrincipal principal);

    AdminUser update(Long id, AdminUser input, AdminPrincipal principal);

    AdminUser changePassword(Long id, String password, AdminPrincipal principal);

    AdminUser changeStatus(Long id, String status, AdminPrincipal principal);
}
