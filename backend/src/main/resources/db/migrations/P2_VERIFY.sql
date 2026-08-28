SELECT COUNT(*) AS history_count FROM order_status_history;
SELECT COUNT(*) AS follow_up_count FROM customer_follow_up;
SELECT COUNT(*) AS policy_count FROM channel_product_policy;
SELECT status, COUNT(*) AS row_count FROM mobile_plan_order GROUP BY status ORDER BY status;
SELECT COUNT(*) AS missing_history_count
FROM mobile_plan_order order_row
LEFT JOIN order_status_history history ON history.order_id = order_row.id
WHERE history.id IS NULL;
SELECT COUNT(*) AS unsupported_status_count
FROM mobile_plan_order
WHERE status NOT IN (
    'PENDING', 'FOLLOWING', 'SUBMITTED_UMALL', 'UNDER_REVIEW',
    'NEED_SUPPLEMENT', 'WAITING_ACTIVATION', 'ACTIVATED', 'COMPLETED',
    'AFTER_SALES', 'CANCELLED'
);
SELECT COUNT(*) AS orphan_follow_up_count
FROM customer_follow_up follow_up
LEFT JOIN customer ON customer.id = follow_up.customer_id
WHERE customer.id IS NULL;
SELECT COUNT(*) AS orphan_policy_count
FROM channel_product_policy policy
LEFT JOIN channel ON channel.id = policy.channel_id
LEFT JOIN mobile_plan plan ON plan.id = policy.plan_id
WHERE channel.id IS NULL OR plan.id IS NULL;
SELECT COUNT(*) AS status_exception_count
FROM cmhk_reconciliation_row
WHERE match_status = 'STATUS_EXCEPTION';
