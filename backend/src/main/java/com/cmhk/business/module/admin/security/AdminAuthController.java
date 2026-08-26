package com.cmhk.business.module.admin.security;

import com.cmhk.business.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理端登录入口。 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminTokenService tokenService;

    /** 使用构造器注入令牌服务。 */
    public AdminAuthController(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** 校验管理员账号密码，并返回后续管理接口所需令牌。 */
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = tokenService.login(request.username(), request.password());
        log.info("管理员登录成功，username={}", request.username());
        return ApiResponse.success(Map.of("token", token, "username", request.username(), "role", "ADMIN"));
    }

    /** 管理员登录请求参数。 */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
