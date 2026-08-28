package com.cmhk.business.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 推荐号码接龙操作历史。 */
@Data
@TableName("referral_number_assignment_history")
public class ReferralNumberHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long chainId;
    private Long referralNumberId;
    private String referralNumber;
    private Long customerId;
    private Long orderId;
    private String actionType;
    private Long operatorUserId;
    private String operatorName;
    private String reason;
    private LocalDateTime createdAt;
}
