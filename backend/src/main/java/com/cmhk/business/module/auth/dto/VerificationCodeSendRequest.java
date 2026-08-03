package com.cmhk.business.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificationCodeSendRequest {
    @NotBlank(message = "入口凭证不能为空")
    private String entryToken;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^[0-9+ -]{6,32}$", message = "手机号格式不正确")
    private String phone;
}
