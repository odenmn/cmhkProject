package com.cmhk.business.module.cashback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 已生成的客户返现计划，规则字段保存生成时快照。 */
@Data
@TableName("customer_cashback_plan")
public class CustomerCashbackPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;

    private Long customerId;

    private Long orderId;

    private Long channelId;

    private Long cashbackRuleId;

    private String ruleSnapshot;

    private LocalDateTime activatedAt;

    private BigDecimal totalAmount;

    private Integer installmentCount;

    private String status;

    private Long generatedByUserId;

    private String generatedByName;

    private LocalDateTime generatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
