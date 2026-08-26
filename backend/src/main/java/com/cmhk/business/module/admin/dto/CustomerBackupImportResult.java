package com.cmhk.business.module.admin.dto;

/** CMHK 客户备份事务导入结果。 */
public record CustomerBackupImportResult(
        Long importId,
        String status,
        int totalRecords,
        int customersCreated,
        int customersReused,
        int ordersCreated,
        int ordersReused,
        int iccidsCreated,
        int iccidsReused,
        int exceptionCount
) {
}
