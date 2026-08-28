package com.cmhk.business.module.mobile.service;

/** 将 CMHK/UMALL 原始状态集中映射为 JOINCOM 标准状态。 */
public interface OrderStatusMappingService {

    MappingResult map(
            String umallStatus,
            String reviewStatus,
            String supplementStatus,
            String activationStatus);

    record MappingResult(String standardStatus, boolean unknown, String reason) {
    }
}
