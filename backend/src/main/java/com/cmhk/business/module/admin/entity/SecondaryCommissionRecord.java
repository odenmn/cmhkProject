package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("secondary_commission_record")
public class SecondaryCommissionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long channelId;
    private Long ruleId;
    private String ruleSnapshot;
    private Integer promotionApplied;
    private BigDecimal joincomTotal;
    private BigDecimal channelGross;
    private BigDecimal channelSubsidy;
    private BigDecimal joincomSubsidy;
    private BigDecimal channelPayable;
    private BigDecimal joincomRetained;
    private BigDecimal t1Amount;
    private BigDecimal t3Amount;
    private BigDecimal t7Amount;
    private BigDecimal adjustmentAmount;
    private BigDecimal finalAmount;
    private String adjustmentReason;
    private String status;
    private String confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
