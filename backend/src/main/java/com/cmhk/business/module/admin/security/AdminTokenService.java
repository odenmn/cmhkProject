package com.cmhk.business.module.admin.security;

import com.cmhk.business.config.AdminProperties;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.service.AdminUserService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

/** 管理员登录校验及签名令牌服务。 */
@Service
public class AdminTokenService {

    private static final String PREFIX = "ADM.";

    private final AdminProperties properties;
    private final AdminUserService adminUserService;

    /** 使用构造器注入管理员账户服务与令牌配置。 */
    public AdminTokenService(AdminProperties properties, AdminUserService adminUserService) {
        this.properties = properties;
        this.adminUserService = adminUserService;
    }

    /** 校验数据库管理员密码并签发有时效的后台访问令牌。 */
    public String login(String username, String password) {
        if (isBlank(properties.getTokenSecret())) {
            throw new IllegalArgumentException("管理员令牌尚未配置，请设置 ADMIN_TOKEN_SECRET");
        }

        AdminUser user = adminUserService.authenticate(username, password);
        long expiresAt = Instant.now()
                .plus(properties.getAccessTokenTtlHours(), ChronoUnit.HOURS)
                .getEpochSecond();
        String scopeId = user.getScopeId() == null ? "" : user.getScopeId().toString();
        String payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((user.getId()
                        + "|" + user.getUsername()
                        + "|" + user.getRoleCode()
                        + "|" + user.getScopeType()
                        + "|" + scopeId
                        + "|" + expiresAt)
                        .getBytes(StandardCharsets.UTF_8));
        return PREFIX + payload + "." + sign(payload);
    }

    /** 验证令牌签名、时效和数据库最新用户状态，成功时返回可信身份。 */
    public Optional<AdminPrincipal> verify(String token) {
        if (token == null || !token.startsWith(PREFIX) || isBlank(properties.getTokenSecret())) {
            return Optional.empty();
        }

        String[] parts = token.substring(PREFIX.length()).split("\\.", 2);
        if (parts.length != 2 || !secureEquals(sign(parts[0]), parts[1])) {
            return Optional.empty();
        }

        try {
            String[] payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
                    .split("\\|", -1);
            if (payload.length != 6 || Long.parseLong(payload[5]) < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            Long userId = Long.parseLong(payload[0]);
            Optional<AdminPrincipal> current = adminUserService.findActivePrincipal(userId);
            if (current.isEmpty() || !sameClaims(current.get(), payload)) {
                return Optional.empty();
            }
            return current;
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private boolean sameClaims(AdminPrincipal principal, String[] payload) {
        String scopeId = principal.scopeId() == null ? "" : principal.scopeId().toString();
        return principal.username().equals(payload[1])
                && principal.roleCode().equals(payload[2])
                && principal.scopeType().equals(payload[3])
                && scopeId.equals(payload[4]);
    }

    /** 使用 HMAC-SHA256 为令牌负载签名。 */
    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("管理员令牌签名失败", exception);
        }
    }

    /** 使用常量时间比较，降低签名比较被侧信道攻击的风险。 */
    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
