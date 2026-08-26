package com.cmhk.business.module.customer.entity;

/**
 * 客户状态数字码。
 *
 * <p>0=待处理，1=跟进中，2=待资料，3=办理中，4=待激活，5=已激活，6=已完成，9=无效。</p>
 */
public final class CustomerStatusCode {

    public static final int PENDING = 0;
    public static final int FOLLOWING = 1;
    public static final int WAITING_DOCUMENTS = 2;
    public static final int PROCESSING = 3;
    public static final int WAITING_ACTIVATION = 4;
    public static final int ACTIVATED = 5;
    public static final int COMPLETED = 6;
    public static final int INVALID = 9;

    private CustomerStatusCode() {
    }

    /** 根据备份阶段和上台日期转换状态；上台后默认待激活，明确已激活时才标记已激活。 */
    public static int fromBackup(String stage, boolean onboarded) {
        if (stage == null || stage.isBlank()) {
            return onboarded ? WAITING_ACTIVATION : PENDING;
        }
        if (stage.contains("无效")) {
            return INVALID;
        }
        if (stage.contains("完成")) {
            return COMPLETED;
        }
        if (stage.contains("已激活")) {
            return ACTIVATED;
        }
        if (onboarded) {
            return WAITING_ACTIVATION;
        }
        if (stage.contains("资料")) {
            return WAITING_DOCUMENTS;
        }
        if (stage.contains("待激活")) {
            return WAITING_ACTIVATION;
        }
        if (stage.contains("寄出") || stage.contains("办理")) {
            return PROCESSING;
        }
        return FOLLOWING;
    }
}
