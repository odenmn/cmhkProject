SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('referral_chain', 'referral_number_pool', 'referral_number_assignment_history')
ORDER BY table_name;

SELECT COUNT(*) AS active_iccid_without_history
FROM iccid_inventory inventory
WHERE inventory.current_order_id IS NOT NULL
  AND inventory.status IN ('ASSIGNED', 'USED')
  AND NOT EXISTS (
      SELECT 1 FROM iccid_assignment_history history WHERE history.iccid_id = inventory.id
  );

SELECT chain_id, SUM(status = 'AVAILABLE') AS available_heads
FROM referral_number_pool
GROUP BY chain_id
HAVING available_heads > 1;

SELECT assigned_order_id, COUNT(*) AS duplicate_count
FROM referral_number_pool
WHERE assigned_order_id IS NOT NULL
GROUP BY assigned_order_id
HAVING COUNT(*) > 1;
