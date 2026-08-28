-- P5-A 只读预检：执行前先确认订单、渠道、套餐和任务表结构。
SHOW CREATE TABLE mobile_plan_order;
SHOW CREATE TABLE mobile_plan;
SHOW CREATE TABLE customer;
SHOW CREATE TABLE channel;
SHOW CREATE TABLE operation_task;

SELECT 'mobile_plan_order' AS table_name, COUNT(*) AS row_count FROM mobile_plan_order
UNION ALL SELECT 'mobile_plan', COUNT(*) FROM mobile_plan
UNION ALL SELECT 'customer', COUNT(*) FROM customer
UNION ALL SELECT 'channel', COUNT(*) FROM channel;

SELECT COUNT(*) AS activated_order_without_activation_time
FROM mobile_plan_order
WHERE (status IN ('ACTIVATED', 'COMPLETED') OR activation_status = '已激活')
  AND status_updated_at IS NOT NULL;

SELECT policy.id AS channel_product_policy_id
FROM channel_product_policy policy
LEFT JOIN channel channel_record ON channel_record.id = policy.channel_id
LEFT JOIN mobile_plan plan ON plan.id = policy.plan_id
WHERE channel_record.id IS NULL OR plan.id IS NULL;
