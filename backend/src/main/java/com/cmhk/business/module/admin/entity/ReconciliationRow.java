package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cmhk_reconciliation_row")
public class ReconciliationRow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long importId;
    @TableField("source_row_number")
    private Integer rowNumber;
    private String rawData;
    private String umallOrderNo;
    private String iccid;
    private String phone;
    private String planName;
    private String umallStatus;
    private String reviewStatus;
    private String supplementStatus;
    private String activationStatus;
    private String contractStatus;
    private BigDecimal commissionAmount;
    private Long matchedOrderId;
    private String matchMethod;
    private String matchStatus;
    private String exceptionReason;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
