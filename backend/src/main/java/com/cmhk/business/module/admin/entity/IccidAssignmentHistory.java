package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("iccid_assignment_history")
public class IccidAssignmentHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long iccidId;
    private String iccid;
    private Long customerId;
    private Long orderId;
    private String actionType;
    private String operatorName;
    private String reason;
    private LocalDateTime createdAt;
}
