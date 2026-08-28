package com.cmhk.business.module.mobile.entity;

import java.util.Arrays;

/** JOINCOM 订单统一办理状态，禁止管理端继续写入自由文本。 */
public enum OrderStatusCode {
    PENDING,
    FOLLOWING,
    SUBMITTED_UMALL,
    UNDER_REVIEW,
    NEED_SUPPLEMENT,
    WAITING_ACTIVATION,
    ACTIVATED,
    COMPLETED,
    AFTER_SALES,
    CANCELLED;

    /** 判断传入状态是否属于受控状态字典。 */
    public static boolean isSupported(String value) {
        return value != null
                && Arrays.stream(values()).anyMatch(item -> item.name().equals(value));
    }
}
