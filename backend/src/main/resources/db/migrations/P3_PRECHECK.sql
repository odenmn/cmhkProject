SELECT COUNT(*) AS referral_chain_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('referral_chain', 'referral_number_pool', 'referral_number_assignment_history');

SELECT status, card_type, COUNT(*) AS total
FROM iccid_inventory
GROUP BY status, card_type
ORDER BY status, card_type;

SELECT COUNT(*) AS active_iccid_without_history
FROM iccid_inventory inventory
WHERE inventory.current_order_id IS NOT NULL
  AND inventory.status IN ('ASSIGNED', 'USED')
  AND NOT EXISTS (
      SELECT 1 FROM iccid_assignment_history history WHERE history.iccid_id = inventory.id
  );

SELECT service_number, COUNT(*) AS duplicate_count
FROM mobile_plan_order
WHERE service_number IS NOT NULL AND service_number <> ''
GROUP BY service_number
HAVING COUNT(*) > 1;
