package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 客户备份确认导入批次。 */
@Data
@TableName("customer_backup_import")
public class CustomerBackupImport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String fileHash;
    private String status;
    private Integer totalCount;
    private Integer customerCount;
    private Integer orderCount;
    private Integer iccidCount;
    private Integer exceptionCount;
    private String operatorName;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
