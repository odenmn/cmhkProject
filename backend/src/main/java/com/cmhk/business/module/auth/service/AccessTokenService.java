package com.cmhk.business.module.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccessTokenService {

    IssuedAccessToken issue(Long customerId);

    Optional<TokenPrincipal> verify(String accessToken);

    record IssuedAccessToken(String value, LocalDateTime expiresAt) {
    }

    record TokenPrincipal(Long customerId, LocalDateTime expiresAt) {
    }
}
