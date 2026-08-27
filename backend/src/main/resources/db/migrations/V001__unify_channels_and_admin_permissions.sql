-- P1：统一渠道主档并扩展管理员角色与数据范围。
-- 执行前必须完成P1_PRECHECK.sql并确认渠道编码冲突清单为空或已人工处理。

ALTER TABLE channel
    ADD COLUMN channel_type VARCHAR(32) NOT NULL DEFAULT 'ORGANIZATION' COMMENT '渠道类型：RESOURCE、ORGANIZATION、ENTERPRISE、SALES_AGENT' AFTER channel_name,
    ADD COLUMN parent_channel_id BIGINT NULL AFTER channel_type,
    ADD COLUMN contact_name VARCHAR(64) NULL AFTER parent_channel_id,
    ADD COLUMN contact_phone VARCHAR(32) NULL AFTER contact_name,
    ADD COLUMN cooperation_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '合作状态：PENDING、ACTIVE、SUSPENDED、ENDED' AFTER contact_phone,
    ADD COLUMN settlement_info VARCHAR(512) NULL AFTER cooperation_status,
    ADD COLUMN owner_user_id BIGINT NULL AFTER settlement_info,
    ADD INDEX idx_channel_parent_id (parent_channel_id),
    ADD INDEX idx_channel_owner_user_id (owner_user_id),
    ADD INDEX idx_channel_cooperation_status (cooperation_status);

UPDATE channel
SET cooperation_status = CASE WHEN enabled = 1 THEN 'ACTIVE' ELSE 'SUSPENDED' END;

CREATE TABLE channel_legacy_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    legacy_table VARCHAR(64) NOT NULL,
    legacy_id BIGINT NOT NULL,
    legacy_channel_code VARCHAR(64) NOT NULL,
    channel_id BIGINT NULL,
    migration_status VARCHAR(16) NOT NULL,
    conflict_reason VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_legacy_source (legacy_table, legacy_id),
    INDEX idx_channel_legacy_channel_id (channel_id),
    INDEX idx_channel_legacy_status (migration_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE channel_migration_exception (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    legacy_table VARCHAR(64) NOT NULL,
    legacy_id BIGINT NOT NULL,
    channel_code VARCHAR(64) NULL,
    exception_type VARCHAR(32) NOT NULL,
    exception_detail VARCHAR(512) NOT NULL,
    resolution_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    resolved_channel_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    UNIQUE KEY uk_channel_migration_exception (legacy_table, legacy_id, exception_type),
    INDEX idx_channel_migration_exception_status (resolution_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO channel_migration_exception (
    legacy_table,
    legacy_id,
    channel_code,
    exception_type,
    exception_detail
)
SELECT 'secondary_channel',
       legacy.id,
       legacy.channel_code,
       'CHANNEL_CODE_CONFLICT',
       CONCAT('统一渠道已存在相同编码，禁止自动覆盖，现有channel.id=', unified.id)
FROM secondary_channel legacy
INNER JOIN channel unified ON unified.channel_code = legacy.channel_code;

INSERT INTO channel (
    channel_code,
    channel_name,
    channel_type,
    contact_name,
    contact_phone,
    cooperation_status,
    settlement_info,
    enabled,
    created_at,
    updated_at
)
SELECT legacy.channel_code,
       legacy.channel_name,
       'ORGANIZATION',
       legacy.contact_name,
       legacy.contact_phone,
       CASE
           WHEN legacy.status = 'ENABLED' THEN 'ACTIVE'
           ELSE 'SUSPENDED'
       END,
       legacy.settlement_info,
       CASE WHEN legacy.status = 'ENABLED' THEN 1 ELSE 0 END,
       legacy.created_at,
       legacy.updated_at
FROM secondary_channel legacy
LEFT JOIN channel unified ON unified.channel_code = legacy.channel_code
WHERE unified.id IS NULL;

INSERT INTO channel_legacy_mapping (
    legacy_table,
    legacy_id,
    legacy_channel_code,
    channel_id,
    migration_status,
    conflict_reason
)
SELECT 'secondary_channel',
       legacy.id,
       legacy.channel_code,
       CASE WHEN exception.id IS NULL THEN unified.id ELSE NULL END,
       CASE WHEN exception.id IS NULL THEN 'MIGRATED' ELSE 'CONFLICT' END,
       CASE WHEN exception.id IS NULL THEN NULL ELSE exception.exception_detail END
FROM secondary_channel legacy
INNER JOIN channel unified ON unified.channel_code = legacy.channel_code
LEFT JOIN channel_migration_exception exception
       ON exception.legacy_table = 'secondary_channel'
      AND exception.legacy_id = legacy.id
      AND exception.exception_type = 'CHANNEL_CODE_CONFLICT';

UPDATE secondary_commission_record record
INNER JOIN channel_legacy_mapping mapping
        ON mapping.legacy_table = 'secondary_channel'
       AND mapping.legacy_id = record.channel_id
       AND mapping.migration_status = 'MIGRATED'
SET record.channel_id = mapping.channel_id;

UPDATE customer customer_record
INNER JOIN customer_channel_binding binding ON binding.customer_id = customer_record.id
SET customer_record.channel_id = binding.channel_id
WHERE customer_record.channel_id IS NULL
   OR customer_record.channel_id <> binding.channel_id;

-- 后台人工建档或导入的数据不一定来自H5入口，允许绑定记录不带entry_id。
ALTER TABLE customer_channel_binding
    MODIFY COLUMN entry_id BIGINT NULL;

ALTER TABLE admin_user
    ADD COLUMN scope_type VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT '数据范围：ALL、CMHK、CHANNEL' AFTER role_code,
    ADD COLUMN scope_id BIGINT NULL COMMENT 'CHANNEL范围对应channel.id，其他范围为空' AFTER scope_type,
    ADD INDEX idx_admin_user_role_scope (role_code, scope_type, scope_id);

UPDATE admin_user
SET role_code = 'ADMIN',
    scope_type = 'ALL',
    scope_id = NULL
WHERE role_code IS NULL OR role_code = '' OR role_code = 'ADMIN';
