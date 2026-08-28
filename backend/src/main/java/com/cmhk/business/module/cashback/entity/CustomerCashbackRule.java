package com.cmhk.business.module.cashback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 按套餐与合约期定义的客户返现规则。 */
@Data
@TableName("customer_cashback_rule")
public class CustomerCashbackRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    private Long planId;

    private Integer contractMonths;

    private BigDecimal installmentAmount;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
