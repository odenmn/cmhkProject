SHOW CREATE TABLE customer;
SHOW CREATE TABLE mobile_plan_order;
SHOW CREATE TABLE cmhk_reconciliation_row;
SHOW CREATE TABLE iccid_inventory;
SHOW CREATE TABLE referral_chain;
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('operation_task', 'operation_task_history');
SELECT match_status, COUNT(*) AS row_count
FROM cmhk_reconciliation_row
GROUP BY match_status
ORDER BY match_status;
SELECT status, card_type, COUNT(*) AS row_count
FROM iccid_inventory
GROUP BY status, card_type
ORDER BY status, card_type;
SELECT COUNT(*) AS active_chain_without_head
FROM referral_chain
WHERE status = 'ACTIVE'
  AND current_head_number_id IS NULL;
