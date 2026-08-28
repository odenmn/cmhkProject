SHOW CREATE TABLE customer_cashback_rule;
SHOW CREATE TABLE customer_cashback_plan;
SHOW CREATE TABLE customer_cashback_installment;

SELECT 'customer_cashback_rule' AS table_name, COUNT(*) AS row_count FROM customer_cashback_rule
UNION ALL SELECT 'customer_cashback_plan', COUNT(*) FROM customer_cashback_plan
UNION ALL SELECT 'customer_cashback_installment', COUNT(*) FROM customer_cashback_installment;

SELECT COUNT(*) AS cashback_plan_order_orphan_count
FROM customer_cashback_plan plan
LEFT JOIN mobile_plan_order order_record ON order_record.id = plan.order_id
WHERE order_record.id IS NULL;

SELECT COUNT(*) AS cashback_installment_plan_orphan_count
FROM customer_cashback_installment installment
LEFT JOIN customer_cashback_plan plan ON plan.id = installment.cashback_plan_id
WHERE plan.id IS NULL;

SELECT COUNT(*) AS cashback_installment_duplicate_count
FROM (
    SELECT cashback_plan_id, installment_no
    FROM customer_cashback_installment
    GROUP BY cashback_plan_id, installment_no
    HAVING COUNT(*) > 1
) duplicated;

SELECT status, COUNT(*) AS row_count
FROM customer_cashback_plan
GROUP BY status
ORDER BY status;

SELECT COUNT(*) AS pending_plan_with_installment_count
FROM customer_cashback_plan plan
JOIN customer_cashback_installment installment ON installment.cashback_plan_id = plan.id
WHERE plan.status = 'PENDING_ACTIVATION';
