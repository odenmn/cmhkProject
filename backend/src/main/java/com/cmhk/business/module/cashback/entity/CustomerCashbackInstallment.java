package com.cmhk.business.module.cashback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 客户返现计划的单月期次，确认只记录内部业务事实。 */
@Data
@TableName("customer_cashback_installment")
public class CustomerCashbackInstallment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cashbackPlanId;

    private Integer installmentNo;

    private BigDecimal plannedAmount;

    private LocalDate plannedDate;

    private String status;

    private Long confirmedByUserId;

    private String confirmedByName;

    private LocalDateTime confirmedAt;

    private String confirmationRemark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
