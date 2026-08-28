-- P2 执行前仅核对结构与数据，不修改数据。
SHOW CREATE TABLE customer;
SHOW CREATE TABLE mobile_plan_order;
SHOW CREATE TABLE mobile_plan;
SHOW CREATE TABLE mobile_plan_offer;
SHOW CREATE TABLE cmhk_reconciliation_row;
SELECT status, COUNT(*) AS row_count FROM mobile_plan_order GROUP BY status ORDER BY status;
SELECT COUNT(*) AS customer_count FROM customer;
SELECT COUNT(*) AS order_count FROM mobile_plan_order;
SELECT COUNT(*) AS plan_count FROM mobile_plan;
SELECT COUNT(*) AS offer_count FROM mobile_plan_offer;
SELECT order_no, COUNT(*) AS duplicate_count
FROM mobile_plan_order
GROUP BY order_no
HAVING COUNT(*) > 1;
SELECT order_source, source_record_id, COUNT(*) AS duplicate_count
FROM mobile_plan_order
WHERE source_record_id IS NOT NULL
GROUP BY order_source, source_record_id
HAVING COUNT(*) > 1;
