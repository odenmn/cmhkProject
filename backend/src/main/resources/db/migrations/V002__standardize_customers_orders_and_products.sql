-- P2：客户、订单与产品标准化。执行前先运行 P2_PRECHECK.sql 并完成数据库备份。

ALTER TABLE customer
    ADD COLUMN owner_user_id BIGINT NULL COMMENT '内部负责人管理员ID' AFTER channel_id,
    ADD INDEX idx_customer_owner_user_id (owner_user_id);

ALTER TABLE mobile_plan_order
    ADD COLUMN review_status VARCHAR(32) NULL COMMENT 'UMALL审核状态' AFTER status,
    ADD COLUMN supplement_status VARCHAR(32) NULL COMMENT 'UMALL补件状态' AFTER review_status,
    ADD COLUMN status_updated_at DATETIME NULL COMMENT '统一办理状态最近更新时间' AFTER umall_status,
    ADD INDEX idx_mobile_plan_order_review_status (review_status),
    ADD INDEX idx_mobile_plan_order_status_updated_at (status_updated_at);

ALTER TABLE cmhk_reconciliation_row
    ADD COLUMN umall_status VARCHAR(64) NULL COMMENT 'CMHK/UMALL原始办理状态' AFTER plan_name;

CREATE TABLE customer_follow_up (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    follow_up_type VARCHAR(32) NOT NULL COMMENT '跟进类型',
    content VARCHAR(1000) NOT NULL COMMENT '跟进内容，不保存正式身份资料',
    next_follow_up_at DATETIME NULL,
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_follow_up_customer (customer_id),
    INDEX idx_customer_follow_up_next_time (next_follow_up_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status_type VARCHAR(32) NOT NULL COMMENT 'JOINCOM、UMALL_REVIEW、UMALL_SUPPLEMENT、ACTIVATION、CONTRACT',
    before_status VARCHAR(32) NULL,
    after_status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL COMMENT 'ADMIN、RECONCILIATION、IMPORT、SYSTEM',
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    remark VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_status_history_order (order_id),
    INDEX idx_order_status_history_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE channel_product_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    promotable TINYINT NOT NULL DEFAULT 1,
    effective_from DATE NULL,
    effective_to DATE NULL,
    cashback_rule_ref VARCHAR(128) NULL,
    commission_rule_ref VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_product_policy (channel_id, plan_id),
    INDEX idx_channel_product_policy_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 先记录旧值与目标值，再更新主表，确保历史状态迁移可追溯。
INSERT INTO order_status_history (
    order_id,
    status_type,
    before_status,
    after_status,
    source_type,
    remark,
    created_at
)
SELECT
    id,
    'JOINCOM',
    status,
    CASE status
        WHEN '待处理' THEN 'PENDING'
        WHEN '跟进中' THEN 'FOLLOWING'
        WHEN 'TRANSFER_TO_AGENT' THEN 'FOLLOWING'
        WHEN '待寄出' THEN 'FOLLOWING'
        WHEN '办理中' THEN 'SUBMITTED_UMALL'
        WHEN '已寄出' THEN 'SUBMITTED_UMALL'
        WHEN '审核中' THEN 'UNDER_REVIEW'
        WHEN '待补件' THEN 'NEED_SUPPLEMENT'
        WHEN '待激活' THEN 'WAITING_ACTIVATION'
        WHEN '已激活' THEN 'ACTIVATED'
        WHEN '已完成' THEN 'COMPLETED'
        WHEN '无效' THEN 'CANCELLED'
        ELSE status
    END,
    'MIGRATION',
    'P2历史订单状态标准化',
    COALESCE(updated_at, created_at)
FROM mobile_plan_order;

UPDATE mobile_plan_order
SET status = CASE status
    WHEN '待处理' THEN 'PENDING'
    WHEN '跟进中' THEN 'FOLLOWING'
    WHEN 'TRANSFER_TO_AGENT' THEN 'FOLLOWING'
    WHEN '待寄出' THEN 'FOLLOWING'
    WHEN '办理中' THEN 'SUBMITTED_UMALL'
    WHEN '已寄出' THEN 'SUBMITTED_UMALL'
    WHEN '审核中' THEN 'UNDER_REVIEW'
    WHEN '待补件' THEN 'NEED_SUPPLEMENT'
    WHEN '待激活' THEN 'WAITING_ACTIVATION'
    WHEN '已激活' THEN 'ACTIVATED'
    WHEN '已完成' THEN 'COMPLETED'
    WHEN '无效' THEN 'CANCELLED'
    ELSE status
END,
status_updated_at = COALESCE(updated_at, created_at);
