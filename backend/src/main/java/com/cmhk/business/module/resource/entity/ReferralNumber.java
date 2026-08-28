package com.cmhk.business.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 接龙内可分配、占用和追溯的推荐号码。 */
@Data
@TableName("referral_number_pool")
public class ReferralNumber {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long chainId;
    private String referralNumber;
    private String status;
    private String sourceType;
    private Long sourceOrderId;
    private String sourceReference;
    private Long previousNumberId;
    private Long nextNumberId;
    private Long assignedCustomerId;
    private Long assignedOrderId;
    private LocalDateTime reservedAt;
    private LocalDateTime usedAt;
    private LocalDateTime disabledAt;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
