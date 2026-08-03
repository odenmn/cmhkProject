package com.cmhk.business.config;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.auth.service.AccessTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenAuthInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_CUSTOMER_ID = "authenticatedCustomerId";
    private static final Logger log = LoggerFactory.getLogger(TokenAuthInterceptor.class);

    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public TokenAuthInterceptor(AccessTokenService accessTokenService, ObjectMapper objectMapper) {
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(request, response, "请先登录或注册");
        }
        return accessTokenService.verify(authorization.substring(7))
                .map(principal -> {
                    request.setAttribute(AUTHENTICATED_CUSTOMER_ID, principal.customerId());
                    return true;
                })
                .orElseGet(() -> unauthorized(request, response, "登录状态已失效，请重新登录"));
    }

    private boolean unauthorized(HttpServletRequest request, HttpServletResponse response, String message) {
        log.info("接口令牌校验失败，path={}，reason={}", request.getRequestURI(), message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
        } catch (Exception exception) {
            log.warn("未授权响应写入失败，path={}", request.getRequestURI());
        }
        return false;
    }
}
