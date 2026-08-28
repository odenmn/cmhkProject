-- P5-A：返现规则、计划和期次。返现易导入将在后续独立迁移中实现。
ALTER TABLE mobile_plan_order
    ADD COLUMN activated_at DATETIME NULL COMMENT '实际激活时间，返现期次从该日满一个月起计算' AFTER status_updated_at,
    ADD INDEX idx_mobile_plan_order_activated_at (activated_at);

CREATE TABLE customer_cashback_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(128) NOT NULL,
    plan_id BIGINT NOT NULL,
    contract_months INT NOT NULL,
    installment_amount DECIMAL(12, 2) NOT NULL,
    effective_from DATE NULL,
    effective_to DATE NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cashback_rule_plan_contract (plan_id, contract_months),
    INDEX idx_cashback_rule_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_cashback_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_no VARCHAR(32) NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    channel_id BIGINT NULL,
    cashback_rule_id BIGINT NOT NULL,
    rule_snapshot JSON NOT NULL,
    activated_at DATETIME NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    installment_count INT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE、COMPLETED、CANCELLED',
    generated_by_user_id BIGINT NULL,
    generated_by_name VARCHAR(64) NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_cashback_plan_no (plan_no),
    UNIQUE KEY uk_customer_cashback_plan_order (order_id),
    INDEX idx_customer_cashback_plan_customer (customer_id),
    INDEX idx_customer_cashback_plan_channel (channel_id),
    INDEX idx_customer_cashback_plan_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_cashback_installment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cashback_plan_id BIGINT NOT NULL,
    installment_no INT NOT NULL,
    planned_amount DECIMAL(12, 2) NOT NULL,
    planned_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING、CONFIRMED、CANCELLED',
    confirmed_by_user_id BIGINT NULL,
    confirmed_by_name VARCHAR(64) NULL,
    confirmed_at DATETIME NULL,
    confirmation_remark VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cashback_installment_sequence (cashback_plan_id, installment_no),
    INDEX idx_cashback_installment_status_date (status, planned_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
