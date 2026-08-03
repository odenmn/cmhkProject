package com.cmhk.business.module.auth.service.impl;

import com.cmhk.business.module.auth.service.AccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

@Service
public class HmacAccessTokenService implements AccessTokenService {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] secret;
    private final long ttlHours;

    public HmacAccessTokenService(@Value("${cmhk.auth.token-secret}") String tokenSecret,
                                  @Value("${cmhk.auth.access-token-ttl-hours:24}") long ttlHours) {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException("未配置本地令牌密钥");
        }
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.ttlHours = ttlHours;
    }

    @Override
    public IssuedAccessToken issue(Long customerId) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);
        String payload = customerId + ":" + expiresAt.toEpochSecond(ZoneOffset.UTC) + ":" + randomNonce();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(encodedPayload));
        return new IssuedAccessToken(encodedPayload + "." + signature, expiresAt);
    }

    @Override
    public Optional<TokenPrincipal> verify(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        String[] parts = accessToken.split("\\.");
        if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), decodeSignature(parts[1]))) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            if (fields.length != 3) {
                return Optional.empty();
            }
            Long customerId = Long.valueOf(fields[0]);
            LocalDateTime expiresAt = LocalDateTime.ofEpochSecond(Long.parseLong(fields[1]), 0, ZoneOffset.UTC);
            if (!expiresAt.isAfter(LocalDateTime.now())) {
                return Optional.empty();
            }
            return Optional.of(new TokenPrincipal(customerId, expiresAt));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA_256));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("令牌签名服务不可用", exception);
        }
    }

    private byte[] decodeSignature(String signature) {
        try {
            return Base64.getUrlDecoder().decode(signature);
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }

    private String randomNonce() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
