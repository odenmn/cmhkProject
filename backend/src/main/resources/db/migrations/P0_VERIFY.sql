-- P0 完成后的只读兼容核验。结果应与 P0_PRECHECK.sql 保存的基线比较。
USE cmhk;

-- P0 不执行真实库 DDL/DML，核心业务数量应保持不变。
SELECT 'customer' AS table_name, COUNT(*) AS row_count FROM customer
UNION ALL SELECT 'mobile_plan_order', COUNT(*) FROM mobile_plan_order
UNION ALL SELECT 'iccid_inventory', COUNT(*) FROM iccid_inventory
UNION ALL SELECT 'cmhk_reconciliation_row', COUNT(*) FROM cmhk_reconciliation_row
UNION ALL SELECT 'secondary_commission_record', COUNT(*) FROM secondary_commission_record;

-- 历史证件号码不得被自动清理；本查询只返回数量。
SELECT COUNT(*) AS nonblank_id_no_count
FROM mobile_plan_order
WHERE id_no IS NOT NULL AND TRIM(id_no) <> '';

-- P0 不得破坏核心关联关系。
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
WHERE records.order_id IS NOT NULL AND orders.id IS NULL;

-- P0 不包含真实库数据修改，数据库回滚动作应为“无需执行”。代码回滚见 P0_ROLLBACK.md。
