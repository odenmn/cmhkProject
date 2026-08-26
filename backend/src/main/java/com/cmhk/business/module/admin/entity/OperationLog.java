package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operatorName;
    private String operationType;
    private String objectType;
    private String objectId;
    private String beforeData;
    private String afterData;
    private String remark;
    private LocalDateTime createdAt;
}
