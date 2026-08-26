package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("iccid_inventory")
public class IccidInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String iccid;
    private String batchNo;
    private String status;
    private Long currentCustomerId;
    private Long currentOrderId;
    private LocalDateTime assignedAt;
    private LocalDateTime usedAt;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
