package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("mobile_plan")
public class MobilePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    private String sourceVersion;

    private String discountFormula;

    private String description;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<MobilePlanOffer> offers;
}
