package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cmhk_reconciliation_import")
public class ReconciliationImport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String fileHash;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer unmatchedCount;
    private String operatorName;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
