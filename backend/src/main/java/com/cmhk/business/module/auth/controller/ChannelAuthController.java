package com.cmhk.business.module.auth.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.auth.dto.ChannelEntryContextResponse;
import com.cmhk.business.module.auth.dto.PhoneLoginRequest;
import com.cmhk.business.module.auth.dto.PhoneLoginResponse;
import com.cmhk.business.module.auth.dto.VerificationCodeSendRequest;
import com.cmhk.business.module.auth.service.ChannelAuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channel-auth")
public class ChannelAuthController {

    private static final Logger log = LoggerFactory.getLogger(ChannelAuthController.class);
    private final ChannelAuthService channelAuthService;

    public ChannelAuthController(ChannelAuthService channelAuthService) {
        this.channelAuthService = channelAuthService;
    }

    @GetMapping("/entry")
    public ApiResponse<ChannelEntryContextResponse> resolveEntry(@RequestParam String entryToken) {
        log.info("开始校验渠道入口");
        try {
            ChannelEntryContextResponse response = channelAuthService.resolveEntry(entryToken);
            log.info("渠道入口校验完成，elderlyMode={}", response.elderlyMode());
            return ApiResponse.success(response);
        } catch (IllegalArgumentException exception) {
            log.info("渠道入口校验失败，原因={}", exception.getMessage());
            return ApiResponse.fail(exception.getMessage());
        }
    }

    @PostMapping("/verification-codes")
    public ApiResponse<Void> sendVerificationCode(@Valid @RequestBody VerificationCodeSendRequest request) {
        log.info("开始发送模拟验证码");
        try {
            channelAuthService.sendMockVerificationCode(request);
            log.info("模拟验证码发送完成");
            return ApiResponse.success(null);
        } catch (IllegalArgumentException exception) {
            log.info("模拟验证码发送失败，原因={}", exception.getMessage());
            return ApiResponse.fail(exception.getMessage());
        }
    }

    @PostMapping("/phone-login")
    public ApiResponse<PhoneLoginResponse> loginByPhone(@Valid @RequestBody PhoneLoginRequest request) {
        log.info("开始手机号验证码登录");
        try {
            PhoneLoginResponse response = channelAuthService.loginByPhone(request);
            log.info("手机号验证码登录完成，customerId={}，newCustomer={}，elderlyMode={}",
                    response.customerId(), response.newCustomer(), response.elderlyMode());
            return ApiResponse.success(response);
        } catch (IllegalArgumentException exception) {
            log.info("手机号验证码登录失败，原因={}", exception.getMessage());
            return ApiResponse.fail(exception.getMessage());
        }
    }
}
