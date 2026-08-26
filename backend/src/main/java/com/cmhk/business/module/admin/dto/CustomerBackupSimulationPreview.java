package com.cmhk.business.module.admin.dto;

import java.util.List;

/**
 * CMHK 客户备份模拟结果。
 *
 * <p>该对象只描述拟生成的数据，不代表数据已经写入数据库。</p>
 */
public record CustomerBackupSimulationPreview(
        String fileHash,
        Summary summary,
        List<String> warnings,
        List<CustomerCandidate> customers,
        List<OrderCandidate> orders,
        List<IccidCandidate> iccids,
        List<ExceptionCandidate> exceptions
) {

    /** 模拟结果汇总，不包含客户敏感字段。 */
    public record Summary(
            int totalRecords,
            int customerCandidates,
            int orderCandidates,
            int onboardedRecords,
            int validRealIccidRows,
            int realIccidCandidates,
            int virtualIccidCandidates,
            int totalIccidCandidates,
            int boundIccidCandidates,
            int exceptionCount
    ) {
    }

    /** 拟生成的客户记录。 */
    public record CustomerCandidate(
            int sourceRowNumber,
            String sourceId,
            String sourceCustomerKey,
            String name,
            String channelName,
            String customerType,
            String sourceCustomerCategory,
            String intendedPlan,
            Integer currentStatus
    ) {
    }

    /** 拟生成的移动套餐订单。 */
    public record OrderCandidate(
            int sourceRowNumber,
            String sourceId,
            String orderNo,
            String sourceCustomerKey,
            String serviceNumber,
            String planName,
            String status,
            String umallStatus,
            String onboardDate,
            boolean onboarded,
            String orderSource
    ) {
    }

    /** 拟录入卡池并绑定客户、订单的 ICCID。 */
    public record IccidCandidate(
            int sourceRowNumber,
            String sourceId,
            String iccid,
            String cardType,
            String status,
            String sourceCustomerKey,
            String orderNo,
            String serviceNumber,
            boolean bound
    ) {
    }

    /** 无法自动处理的来源行。敏感值只返回脱敏结果。 */
    public record ExceptionCandidate(
            int sourceRowNumber,
            String sourceId,
            String code,
            String message,
            String maskedValue
    ) {
    }
}
