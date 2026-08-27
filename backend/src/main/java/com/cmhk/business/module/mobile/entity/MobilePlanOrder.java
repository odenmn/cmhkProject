package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private Long customerId;

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

    /**
     * 历史证件号码字段仅保留数据库兼容，不允许通过接口接收或返回。
     */
    @JsonIgnore
    private String idNo;

    private String referrerPhone;

    private String preferredContactTime;

    private String remark;

    private String status;

    private String umallOrderNo;

    private String serviceNumber;

    private String activationStatus;

    private String contractStatus;

    private String orderSource;

    private String reconciliationStatus;

    private String sourceRecordId;

    private String sourceChannelName;

    private String umallStatus;

    private LocalDate onboardDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
