package com.cmhk.business.module.customer.dto;

public record PhoneLoginResponse(
        Long customerId,
        Boolean newCustomer,
        String channelName,
        Integer elderlyMode,
        String accessToken,
        String expiresAt
) {
}
