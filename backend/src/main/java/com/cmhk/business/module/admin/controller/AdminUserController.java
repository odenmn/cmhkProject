package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 仅系统管理员可用的后台用户管理接口。 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AdminUser>> list(
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.list(principal));
    }

    @PostMapping
    public ApiResponse<AdminUser> create(
            @RequestBody CreateRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        AdminUser user = toUser(
                request.username(),
                request.displayName(),
                request.phone(),
                request.email(),
                request.roleCode(),
                request.scopeType(),
                request.scopeId());
        return ApiResponse.success(service.create(user, request.password(), principal));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUser> update(
            @PathVariable Long id,
            @RequestBody UpdateRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        AdminUser user = toUser(
                null,
                request.displayName(),
                request.phone(),
                request.email(),
                request.roleCode(),
                request.scopeType(),
                request.scopeId());
        return ApiResponse.success(service.update(id, user, principal));
    }

    @PostMapping("/{id}/password")
    public ApiResponse<AdminUser> password(
            @PathVariable Long id,
            @RequestBody PasswordRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.changePassword(id, request.password(), principal));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AdminUser> status(
            @PathVariable Long id,
            @RequestBody StatusRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.changeStatus(id, request.status(), principal));
    }

    private AdminUser toUser(
            String username,
            String displayName,
            String phone,
            String email,
            String roleCode,
            String scopeType,
            Long scopeId) {
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRoleCode(roleCode);
        user.setScopeType(scopeType);
        user.setScopeId(scopeId);
        return user;
    }

    public record CreateRequest(
            @NotBlank String username,
            @NotBlank String displayName,
            String phone,
            String email,
            @NotBlank String password,
            @NotBlank String roleCode,
            @NotBlank String scopeType,
            Long scopeId) {}

    public record UpdateRequest(
            @NotBlank String displayName,
            String phone,
            String email,
            @NotBlank String roleCode,
            @NotBlank String scopeType,
            Long scopeId) {}

    public record PasswordRequest(@NotBlank String password) {}

    public record StatusRequest(@NotBlank String status) {}
}
