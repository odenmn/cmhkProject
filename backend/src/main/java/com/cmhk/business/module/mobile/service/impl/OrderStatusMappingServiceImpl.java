package com.cmhk.business.module.mobile.service.impl;

import com.cmhk.business.module.mobile.entity.OrderStatusCode;
import com.cmhk.business.module.mobile.service.OrderStatusMappingService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/** P2 状态映射器；无法识别时明确返回异常，调用方不得覆盖订单。 */
@Service
public class OrderStatusMappingServiceImpl implements OrderStatusMappingService {

    private static final Map<String, String> UMALL_STATUS_MAPPING = Map.ofEntries(
            Map.entry("待处理", OrderStatusCode.PENDING.name()),
            Map.entry("跟进中", OrderStatusCode.FOLLOWING.name()),
            Map.entry("transfer_to_agent", OrderStatusCode.FOLLOWING.name()),
            Map.entry("待寄出", OrderStatusCode.FOLLOWING.name()),
            Map.entry("已提交umall", OrderStatusCode.SUBMITTED_UMALL.name()),
            Map.entry("已寄出", OrderStatusCode.SUBMITTED_UMALL.name()),
            Map.entry("审核中", OrderStatusCode.UNDER_REVIEW.name()),
            Map.entry("待补件", OrderStatusCode.NEED_SUPPLEMENT.name()),
            Map.entry("待激活", OrderStatusCode.WAITING_ACTIVATION.name()),
            Map.entry("已激活", OrderStatusCode.ACTIVATED.name()),
            Map.entry("已完成", OrderStatusCode.COMPLETED.name()),
            Map.entry("售后中", OrderStatusCode.AFTER_SALES.name()),
            Map.entry("已取消", OrderStatusCode.CANCELLED.name()),
            Map.entry("pending", OrderStatusCode.PENDING.name()),
            Map.entry("submitted", OrderStatusCode.SUBMITTED_UMALL.name()),
            Map.entry("under_review", OrderStatusCode.UNDER_REVIEW.name()),
            Map.entry("need_supplement", OrderStatusCode.NEED_SUPPLEMENT.name()),
            Map.entry("waiting_activation", OrderStatusCode.WAITING_ACTIVATION.name()),
            Map.entry("activated", OrderStatusCode.ACTIVATED.name()),
            Map.entry("completed", OrderStatusCode.COMPLETED.name()),
            Map.entry("cancelled", OrderStatusCode.CANCELLED.name())
    );

    @Override
    public MappingResult map(
            String umallStatus,
            String reviewStatus,
            String supplementStatus,
            String activationStatus) {
        String normalizedActivation = normalize(activationStatus);
        if (containsAny(normalizedActivation, "待激活", "未激活", "waiting activation", "pending activation", "not activated")) {
            return success(OrderStatusCode.WAITING_ACTIVATION);
        }
        if (containsAny(normalizedActivation, "已激活", "activated", "activation success")) {
            return success(OrderStatusCode.ACTIVATED);
        }

        String normalizedSupplement = normalize(supplementStatus);
        boolean neutralSupplement = containsAny(
                normalizedSupplement,
                "无",
                "无需",
                "否",
                "none",
                "not required");
        if (hasText(normalizedSupplement) && !neutralSupplement) {
            return success(OrderStatusCode.NEED_SUPPLEMENT);
        }

        String normalizedReview = normalize(reviewStatus);
        if (containsAny(normalizedReview, "通过", "审核通过", "approved", "passed")) {
            return success(OrderStatusCode.WAITING_ACTIVATION);
        }
        if (containsAny(normalizedReview, "审核中", "待审核", "under review", "pending review")) {
            return success(OrderStatusCode.UNDER_REVIEW);
        }

        String normalizedUmall = normalize(umallStatus);
        if (hasText(normalizedUmall)) {
            String mapped = UMALL_STATUS_MAPPING.get(normalizedUmall);
            if (mapped != null) {
                return new MappingResult(mapped, false, null);
            }
        }

        if (hasText(normalizedActivation)
                || hasText(normalizedReview)
                || hasText(normalizedSupplement) && !neutralSupplement
                || hasText(normalizedUmall)) {
            return new MappingResult(null, true, "存在无法识别的 CMHK/UMALL 状态，未覆盖订单办理状态");
        }
        return new MappingResult(null, false, null);
    }

    private MappingResult success(OrderStatusCode status) {
        return new MappingResult(status.name(), false, null);
    }

    private boolean containsAny(String source, String... candidates) {
        if (!hasText(source)) {
            return false;
        }
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
