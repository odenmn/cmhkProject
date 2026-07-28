package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mobile_plan_order")
public class MobilePlanOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long planId;

    private String planCode;

    private String planName;

    private String planType;

    private BigDecimal monthlyFee;

    private String channelPriceText;

    private BigDecimal effectiveMonthlyFee;

    private String effectivePriceText;

    private BigDecimal officialMonthlyFee;

    private String officialPriceText;

    private String dataQuota;

    private String voiceQuota;

    private String roamingBenefit;

    private String contractPeriod;

    private LocalDate promotionEndDate;

    private String discountFormula;

    private String customerName;

    private String contactPhone;

    private Integer customerIdentity;

    private Integer hasOffer;

    private Integer hasPassOrHkid;

    private LocalDate expectedStartDate;

    private String idType;

    private String idNo;

    private String referrerPhone;

    private String preferredContactTime;

    private String remark;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
