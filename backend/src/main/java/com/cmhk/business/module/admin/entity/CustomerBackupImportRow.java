package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 客户备份导入结果或异常明细，不保存源文件原文。 */
@Data
@TableName("customer_backup_import_row")
public class CustomerBackupImportRow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long importId;
    private Integer sourceRowNumber;
    private String sourceId;
    private Long customerId;
    private Long orderId;
    private Long iccidId;
    private String resultStatus;
    private String exceptionCode;
    private String exceptionReason;
    private LocalDateTime createdAt;
}
