-- V006及现有订单批量生成前只读检查。
SHOW CREATE TABLE customer_cashback_plan;

SELECT COUNT(*) AS existing_cashback_plan_count
FROM customer_cashback_plan;

SELECT plan_id, plan_name, status, COUNT(*) AS order_count
FROM mobile_plan_order
GROUP BY plan_id, plan_name, status
ORDER BY order_count DESC, plan_name;

SELECT COUNT(*) AS order_without_customer_count
FROM mobile_plan_order order_record
LEFT JOIN customer customer_record ON customer_record.id = order_record.customer_id
WHERE customer_record.id IS NULL;
