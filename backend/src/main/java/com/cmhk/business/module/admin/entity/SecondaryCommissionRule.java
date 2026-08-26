package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("secondary_commission_rule")
public class SecondaryCommissionRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String planCode;
    private String planName;
    private BigDecimal monthlyFee;
    private Integer contractMonths;
    private BigDecimal mainMultiplier;
    private BigDecimal extraMultiplier;
    private BigDecimal promotionMultiplier;
    private BigDecimal channelMultiplier;
    private BigDecimal defaultChannelSubsidy;
    private BigDecimal defaultJoincomSubsidy;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
