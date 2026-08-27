package com.cmhk.business.config;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.admin.security.AdminPermissionPolicy;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.security.AdminTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ADMIN_USERNAME = "authenticatedAdminUsername";
    public static final String ADMIN_PRINCIPAL = "authenticatedAdminPrincipal";

    private final AdminTokenService tokenService;
    private final AdminPermissionPolicy permissionPolicy;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(
            AdminTokenService tokenService,
            AdminPermissionPolicy permissionPolicy,
            ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.permissionPolicy = permissionPolicy;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            var verified = tokenService.verify(authorization.substring(7));
            if (verified.isPresent()) {
                AdminPrincipal principal = verified.get();
                if (!permissionPolicy.isAllowed(principal, request.getMethod(), request.getRequestURI())) {
                    writeFailure(response, HttpServletResponse.SC_FORBIDDEN, "当前账号没有此操作权限");
                    return false;
                }
                request.setAttribute(ADMIN_PRINCIPAL, principal);
                request.setAttribute(ADMIN_USERNAME, principal.username());
                return true;
            }
        }
        writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "管理员登录状态已失效");
        return false;
    }

    private void writeFailure(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
    }
}
