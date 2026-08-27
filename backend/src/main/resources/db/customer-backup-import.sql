-- 历史升级脚本：CMHK 客户备份模拟导入。
-- 仅用于追溯已经执行过的升级，不得作为当前真实数据库的日常升级入口重复执行。
-- 后续真实库升级统一使用 db/migrations 下按序编号的增量脚本。
ALTER TABLE customer MODIFY phone VARCHAR(32) NULL;
ALTER TABLE customer MODIFY phone_verified_at DATETIME NULL;
ALTER TABLE mobile_plan_order MODIFY plan_code VARCHAR(64) NULL;
ALTER TABLE mobile_plan_order MODIFY plan_name VARCHAR(128) NULL;
ALTER TABLE mobile_plan_order MODIFY monthly_fee DECIMAL(10, 2) NULL;
ALTER TABLE mobile_plan_order MODIFY contact_phone VARCHAR(32) NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512),
    IN after_column VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = target_table AND column_name = target_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', target_table, ' ADD COLUMN ', target_column, ' ', column_definition,
            IF(after_column IS NULL OR after_column = '', '', CONCAT(' AFTER ', after_column)));
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('customer', 'source_system', 'VARCHAR(32) NULL', 'current_status');
CALL add_column_if_missing('customer', 'source_customer_id', 'VARCHAR(64) NULL', 'source_system');
CALL add_column_if_missing('customer', 'customer_category', 'VARCHAR(32) NULL COMMENT ''业务客户类别，例如留学生、地产客户、研究生''', 'customer_type');
CALL add_column_if_missing('mobile_plan_order', 'source_record_id', 'VARCHAR(64) NULL', 'reconciliation_status');
CALL add_column_if_missing('mobile_plan_order', 'source_channel_name', 'VARCHAR(128) NULL', 'source_record_id');
CALL add_column_if_missing('mobile_plan_order', 'umall_status', 'VARCHAR(32) NULL', 'source_channel_name');
CALL add_column_if_missing('mobile_plan_order', 'onboard_date', 'DATE NULL', 'umall_status');
CALL add_column_if_missing('iccid_inventory', 'card_type', 'VARCHAR(16) NOT NULL DEFAULT ''REAL''', 'remark');
CALL add_column_if_missing('iccid_inventory', 'service_number', 'VARCHAR(32) NULL', 'card_type');
CALL add_column_if_missing('iccid_inventory', 'source_system', 'VARCHAR(32) NULL', 'service_number');
CALL add_column_if_missing('iccid_inventory', 'source_record_id', 'VARCHAR(64) NULL', 'source_system');
CALL add_column_if_missing('iccid_inventory', 'replaced_by_iccid_id', 'BIGINT NULL', 'source_record_id');
CALL add_column_if_missing('iccid_inventory', 'replaced_at', 'DATETIME NULL', 'replaced_by_iccid_id');
DROP PROCEDURE IF EXISTS add_column_if_missing;

-- 将历史导入时暂存在需求摘要中的客户类别拆出，并统一客户状态数字码。
UPDATE customer
SET customer_category = TRIM(SUBSTRING(requirement_summary, CHAR_LENGTH('来源客户类型：') + 1))
WHERE source_system = 'CMHK_BACKUP'
  AND customer_category IS NULL
  AND requirement_summary LIKE '来源客户类型：%';

UPDATE customer
SET requirement_summary = NULL
WHERE source_system = 'CMHK_BACKUP'
  AND requirement_summary LIKE '来源客户类型：%';

UPDATE mobile_plan_order
SET activation_status = CASE
    WHEN status LIKE '%已激活%' THEN '已激活'
    WHEN onboard_date IS NOT NULL OR status LIKE '%待激活%' THEN '待激活'
    ELSE NULL
END
WHERE order_source = 'CMHK_BACKUP';

UPDATE customer c
SET current_status = CASE
    WHEN current_status LIKE '%无效%' THEN '9'
    WHEN current_status LIKE '%完成%' THEN '6'
    WHEN current_status LIKE '%已激活%' THEN '5'
    WHEN EXISTS (
        SELECT 1
        FROM mobile_plan_order o
        WHERE o.customer_id = c.id
          AND o.order_source = 'CMHK_BACKUP'
          AND o.onboard_date IS NOT NULL
    ) THEN '4'
    WHEN current_status LIKE '%资料%' THEN '2'
    WHEN current_status LIKE '%待激活%' THEN '4'
    WHEN current_status LIKE '%寄出%' OR current_status LIKE '%办理%' THEN '3'
    WHEN current_status IS NULL OR current_status = '' OR current_status = '待处理' THEN '0'
    ELSE '1'
END;

ALTER TABLE customer
    MODIFY current_status TINYINT NOT NULL DEFAULT 0
    COMMENT '客户状态码：0待处理，1跟进中，2待资料，3办理中，4待激活，5已激活，6已完成，9无效';

DROP PROCEDURE IF EXISTS add_unique_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_unique_index_if_missing(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64),
    IN index_definition VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = target_table AND index_name = target_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', target_table, ' ADD UNIQUE INDEX ', target_index, ' ', index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_unique_index_if_missing('customer', 'uk_customer_source', '(source_system, source_customer_id)');
CALL add_unique_index_if_missing('mobile_plan_order', 'uk_order_source_record', '(order_source, source_record_id)');
DROP PROCEDURE IF EXISTS add_unique_index_if_missing;

CREATE TABLE IF NOT EXISTS customer_backup_import (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    customer_count INT NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    iccid_count INT NOT NULL DEFAULT 0,
    exception_count INT NOT NULL DEFAULT 0,
    operator_name VARCHAR(64) NOT NULL,
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_backup_file_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_backup_import_row (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_id BIGINT NOT NULL,
    source_row_number INT NOT NULL,
    source_id VARCHAR(64),
    customer_id BIGINT,
    order_id BIGINT,
    iccid_id BIGINT,
    result_status VARCHAR(24) NOT NULL,
    exception_code VARCHAR(64),
    exception_reason VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_backup_row_import (import_id),
    INDEX idx_customer_backup_row_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
