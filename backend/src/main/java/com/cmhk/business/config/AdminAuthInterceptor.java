package com.cmhk.business.config;

import com.cmhk.business.common.ApiResponse;
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
    private final AdminTokenService tokenService;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(AdminTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            var username = tokenService.verify(authorization.substring(7));
            if (username.isPresent()) {
                request.setAttribute(ADMIN_USERNAME, username.get());
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("管理员登录状态已失效"));
        return false;
    }
}
