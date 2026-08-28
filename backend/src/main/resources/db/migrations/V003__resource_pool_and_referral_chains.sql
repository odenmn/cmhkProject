-- P3：资源池与多接龙推荐号码。执行前先运行 P3_PRECHECK.sql 并完成数据库备份。

CREATE TABLE referral_chain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chain_code VARCHAR(64) NOT NULL COMMENT '接龙编码',
    chain_name VARCHAR(128) NOT NULL COMMENT '接龙名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/CLOSED',
    current_head_number_id BIGINT NULL COMMENT '当前龙头推荐号码记录ID',
    operator_name VARCHAR(64) NULL,
    remark VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_referral_chain_code (chain_code),
    UNIQUE KEY uk_referral_chain_head (current_head_number_id),
    INDEX idx_referral_chain_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE referral_number_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chain_id BIGINT NOT NULL COMMENT '所属接龙ID',
    referral_number VARCHAR(32) NOT NULL COMMENT '推荐号码/上台号码',
    status VARCHAR(16) NOT NULL DEFAULT 'DISABLED' COMMENT 'AVAILABLE/RESERVED/USED/DISABLED',
    source_type VARCHAR(32) NOT NULL COMMENT 'ORDER/CONFIRMED_IMPORT',
    source_order_id BIGINT NULL COMMENT '号码来源订单ID',
    source_reference VARCHAR(128) NULL COMMENT '导入文件摘要或来源标识',
    previous_number_id BIGINT NULL COMMENT '接龙中的上一号码',
    next_number_id BIGINT NULL COMMENT '接龙中的下一号码',
    assigned_customer_id BIGINT NULL,
    assigned_order_id BIGINT NULL,
    reserved_at DATETIME NULL,
    used_at DATETIME NULL,
    disabled_at DATETIME NULL,
    operator_name VARCHAR(64) NULL,
    remark VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_referral_number (referral_number),
    UNIQUE KEY uk_referral_assigned_order (assigned_order_id),
    INDEX idx_referral_number_chain_status (chain_id, status),
    INDEX idx_referral_number_source_order (source_order_id),
    INDEX idx_referral_number_customer (assigned_customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE referral_number_assignment_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chain_id BIGINT NOT NULL,
    referral_number_id BIGINT NOT NULL,
    referral_number VARCHAR(32) NOT NULL,
    customer_id BIGINT NULL,
    order_id BIGINT NULL,
    action_type VARCHAR(32) NOT NULL COMMENT 'CHAIN_CREATE/DESIGNATE_HEAD/RESERVE/RELEASE/USE/BECOME_HEAD/DISABLE/IMPORT',
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    reason VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_referral_history_chain (chain_id),
    INDEX idx_referral_history_number (referral_number_id),
    INDEX idx_referral_history_order (order_id),
    INDEX idx_referral_history_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 只为现存绑定关系补充历史基线，不改变卡池当前状态或绑定关系。
INSERT INTO iccid_assignment_history (
    iccid_id,
    iccid,
    customer_id,
    order_id,
    action_type,
    operator_name,
    reason,
    created_at
)
SELECT
    inventory.id,
    inventory.iccid,
    inventory.current_customer_id,
    inventory.current_order_id,
    'MIGRATION_BASELINE',
    'P3_MIGRATION',
    'P3迁移前现有ICCID绑定基线',
    COALESCE(inventory.assigned_at, inventory.created_at, CURRENT_TIMESTAMP)
FROM iccid_inventory inventory
WHERE inventory.current_order_id IS NOT NULL
  AND inventory.status IN ('ASSIGNED', 'USED')
  AND NOT EXISTS (
      SELECT 1
      FROM iccid_assignment_history history
      WHERE history.iccid_id = inventory.id
  );
