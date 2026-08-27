-- P0 执行前只读检查。执行结果必须保存，不得修改或展示证件号码原文。
USE cmhk;

-- 保存当前核心表结构。
SHOW CREATE TABLE customer;
SHOW CREATE TABLE customer_channel_binding;
SHOW CREATE TABLE mobile_plan_order;
SHOW CREATE TABLE iccid_inventory;
SHOW CREATE TABLE iccid_assignment_history;
SHOW CREATE TABLE cmhk_reconciliation_import;
SHOW CREATE TABLE cmhk_reconciliation_row;
SHOW CREATE TABLE secondary_channel;
SHOW CREATE TABLE secondary_commission_rule;
SHOW CREATE TABLE secondary_commission_record;
SHOW CREATE TABLE customer_backup_import;
SHOW CREATE TABLE customer_backup_import_row;
SHOW CREATE TABLE operation_log;

-- 精确数据量基线。
SELECT 'customer' AS table_name, COUNT(*) AS row_count FROM customer
UNION ALL SELECT 'mobile_plan_order', COUNT(*) FROM mobile_plan_order
UNION ALL SELECT 'iccid_inventory', COUNT(*) FROM iccid_inventory
UNION ALL SELECT 'iccid_assignment_history', COUNT(*) FROM iccid_assignment_history
UNION ALL SELECT 'cmhk_reconciliation_import', COUNT(*) FROM cmhk_reconciliation_import
UNION ALL SELECT 'cmhk_reconciliation_row', COUNT(*) FROM cmhk_reconciliation_row
UNION ALL SELECT 'secondary_channel', COUNT(*) FROM secondary_channel
UNION ALL SELECT 'secondary_commission_rule', COUNT(*) FROM secondary_commission_rule
UNION ALL SELECT 'secondary_commission_record', COUNT(*) FROM secondary_commission_record
UNION ALL SELECT 'customer_backup_import', COUNT(*) FROM customer_backup_import
UNION ALL SELECT 'customer_backup_import_row', COUNT(*) FROM customer_backup_import_row
UNION ALL SELECT 'operation_log', COUNT(*) FROM operation_log;

-- 敏感字段只统计影响数量，不返回字段内容。
SELECT
    COUNT(*) AS order_count,
    SUM(CASE WHEN id_no IS NOT NULL AND TRIM(id_no) <> '' THEN 1 ELSE 0 END) AS nonblank_id_no_count,
    SUM(CASE WHEN id_type IS NOT NULL AND TRIM(id_type) <> '' THEN 1 ELSE 0 END) AS nonblank_id_type_count
FROM mobile_plan_order;

SELECT COUNT(*) AS operation_log_rows_with_id_no_key
FROM operation_log
WHERE COALESCE(before_data, '') LIKE '%"idNo"%'
   OR COALESCE(after_data, '') LIKE '%"idNo"%';

-- 关键唯一性检查，只返回重复组数量。
SELECT 'order_no' AS check_name, COUNT(*) AS duplicate_group_count
FROM (SELECT order_no FROM mobile_plan_order WHERE order_no IS NOT NULL GROUP BY order_no HAVING COUNT(*) > 1) duplicated
UNION ALL
SELECT 'customer_source', COUNT(*)
FROM (SELECT source_system, source_customer_id FROM customer WHERE source_system IS NOT NULL AND source_customer_id IS NOT NULL GROUP BY source_system, source_customer_id HAVING COUNT(*) > 1) duplicated
UNION ALL
SELECT 'order_source', COUNT(*)
FROM (SELECT order_source, source_record_id FROM mobile_plan_order WHERE order_source IS NOT NULL AND source_record_id IS NOT NULL GROUP BY order_source, source_record_id HAVING COUNT(*) > 1) duplicated
UNION ALL
SELECT 'iccid', COUNT(*)
FROM (SELECT iccid FROM iccid_inventory WHERE iccid IS NOT NULL GROUP BY iccid HAVING COUNT(*) > 1) duplicated
UNION ALL
SELECT 'reconciliation_file_hash', COUNT(*)
FROM (SELECT file_hash FROM cmhk_reconciliation_import WHERE file_hash IS NOT NULL GROUP BY file_hash HAVING COUNT(*) > 1) duplicated
UNION ALL
SELECT 'commission_order', COUNT(*)
FROM (SELECT order_id FROM secondary_commission_record WHERE order_id IS NOT NULL GROUP BY order_id HAVING COUNT(*) > 1) duplicated;

-- 兼容性空值基线，不修改已有模拟数据。
SELECT
    SUM(CASE WHEN customer_id IS NULL THEN 1 ELSE 0 END) AS null_customer_id_count,
    SUM(CASE WHEN plan_code IS NULL OR TRIM(plan_code) = '' THEN 1 ELSE 0 END) AS null_plan_code_count,
    SUM(CASE WHEN monthly_fee IS NULL THEN 1 ELSE 0 END) AS null_monthly_fee_count,
    SUM(CASE WHEN contact_phone IS NULL OR TRIM(contact_phone) = '' THEN 1 ELSE 0 END) AS null_contact_phone_count
FROM mobile_plan_order;

-- 核心悬空关系检查。
SELECT 'order_customer' AS check_name, COUNT(*) AS orphan_count
FROM mobile_plan_order orders LEFT JOIN customer customers ON customers.id = orders.customer_id
WHERE orders.customer_id IS NOT NULL AND customers.id IS NULL
UNION ALL
SELECT 'iccid_customer', COUNT(*)
FROM iccid_inventory cards LEFT JOIN customer customers ON customers.id = cards.current_customer_id
WHERE cards.current_customer_id IS NOT NULL AND customers.id IS NULL
UNION ALL
SELECT 'iccid_order', COUNT(*)
FROM iccid_inventory cards LEFT JOIN mobile_plan_order orders ON orders.id = cards.current_order_id
WHERE cards.current_order_id IS NOT NULL AND orders.id IS NULL
UNION ALL
SELECT 'reconciliation_order', COUNT(*)
FROM cmhk_reconciliation_row reconciliation_rows
LEFT JOIN mobile_plan_order orders ON orders.id = reconciliation_rows.matched_order_id
WHERE reconciliation_rows.matched_order_id IS NOT NULL AND orders.id IS NULL
UNION ALL
SELECT 'commission_order', COUNT(*)
FROM secondary_commission_record records LEFT JOIN mobile_plan_order orders ON orders.id = records.order_id
WHERE records.order_id IS NOT NULL AND orders.id IS NULL
UNION ALL
SELECT 'binding_customer', COUNT(*)
FROM customer_channel_binding bindings LEFT JOIN customer customers ON customers.id = bindings.customer_id
WHERE customers.id IS NULL
UNION ALL
SELECT 'binding_channel', COUNT(*)
FROM customer_channel_binding bindings LEFT JOIN channel channels ON channels.id = bindings.channel_id
WHERE channels.id IS NULL;
