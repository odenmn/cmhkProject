package com.cmhk.business.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("phone_verification_code")
public class PhoneVerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String codeHash;
    private Integer attemptCount;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private String status;
    private LocalDateTime createdAt;
}
