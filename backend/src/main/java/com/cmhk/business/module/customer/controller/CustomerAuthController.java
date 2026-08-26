package com.cmhk.business.module.customer.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.channel.dto.ChannelEntryContextResponse;
import com.cmhk.business.module.customer.dto.PhoneLoginRequest;
import com.cmhk.business.module.customer.dto.PhoneLoginResponse;
import com.cmhk.business.module.customer.dto.VerificationCodeSendRequest;
import com.cmhk.business.module.customer.service.CustomerChannelAuthService;
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
/** 客户侧渠道入口校验与手机号登录接口，保留原 URL 兼容已有 H5。 */
public class CustomerAuthController {

    private static final Logger log = LoggerFactory.getLogger(CustomerAuthController.class);
    private final CustomerChannelAuthService customerChannelAuthService;

    public CustomerAuthController(CustomerChannelAuthService customerChannelAuthService) {
        this.customerChannelAuthService = customerChannelAuthService;
    }

    @GetMapping("/entry")
    public ApiResponse<ChannelEntryContextResponse> resolveEntry(@RequestParam String entryToken) {
        log.info("开始校验渠道入口");
        ChannelEntryContextResponse response = customerChannelAuthService.resolveEntry(entryToken);
        log.info("渠道入口校验完成，elderlyMode={}", response.elderlyMode());
        return ApiResponse.success(response);
    }

    @PostMapping("/verification-codes")
    public ApiResponse<Void> sendVerificationCode(@Valid @RequestBody VerificationCodeSendRequest request) {
        log.info("开始发送模拟验证码");
        customerChannelAuthService.sendMockVerificationCode(request);
        log.info("模拟验证码发送完成");
        return ApiResponse.success(null);
    }

    @PostMapping("/phone-login")
    public ApiResponse<PhoneLoginResponse> loginByPhone(@Valid @RequestBody PhoneLoginRequest request) {
        log.info("开始手机号验证码登录");
        PhoneLoginResponse response = customerChannelAuthService.loginByPhone(request);
        log.info("手机号验证码登录完成，customerId={}，newCustomer={}，elderlyMode={}",
                response.customerId(), response.newCustomer(), response.elderlyMode());
        return ApiResponse.success(response);
    }
}
