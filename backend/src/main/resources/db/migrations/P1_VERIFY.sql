-- P1执行后核验：所有查询均为只读。

SHOW CREATE TABLE channel;
SHOW CREATE TABLE channel_legacy_mapping;
SHOW CREATE TABLE channel_migration_exception;
SHOW CREATE TABLE secondary_commission_record;
SHOW CREATE TABLE admin_user;

SELECT 'channel' AS table_name, COUNT(*) AS row_count FROM channel
UNION ALL SELECT 'secondary_channel', COUNT(*) FROM secondary_channel
UNION ALL SELECT 'channel_legacy_mapping', COUNT(*) FROM channel_legacy_mapping
UNION ALL SELECT 'channel_migration_exception', COUNT(*) FROM channel_migration_exception
UNION ALL SELECT 'secondary_commission_record', COUNT(*) FROM secondary_commission_record
UNION ALL SELECT 'customer', COUNT(*) FROM customer
UNION ALL SELECT 'customer_channel_binding', COUNT(*) FROM customer_channel_binding
UNION ALL SELECT 'admin_user', COUNT(*) FROM admin_user;

SELECT migration_status, COUNT(*) AS row_count
FROM channel_legacy_mapping
GROUP BY migration_status;

SELECT exception_type, resolution_status, COUNT(*) AS row_count
FROM channel_migration_exception
GROUP BY exception_type, resolution_status;

SELECT COUNT(*) AS unmapped_legacy_channel_count
FROM secondary_channel legacy
LEFT JOIN channel_legacy_mapping mapping
       ON mapping.legacy_table = 'secondary_channel'
      AND mapping.legacy_id = legacy.id
WHERE mapping.id IS NULL;

SELECT COUNT(*) AS commission_unified_channel_orphan_count
FROM secondary_commission_record record
LEFT JOIN channel unified ON unified.id = record.channel_id
WHERE unified.id IS NULL;

SELECT COUNT(*) AS customer_binding_mismatch_count
FROM customer customer_record
INNER JOIN customer_channel_binding binding ON binding.customer_id = customer_record.id
WHERE customer_record.channel_id IS NULL
   OR customer_record.channel_id <> binding.channel_id;

SELECT COUNT(*) AS invalid_admin_role_or_scope_count
FROM admin_user
WHERE role_code NOT IN ('ADMIN', 'OPERATOR')
   OR scope_type NOT IN ('ALL', 'CMHK', 'CHANNEL')
   OR (scope_type = 'CHANNEL' AND scope_id IS NULL)
   OR (scope_type <> 'CHANNEL' AND scope_id IS NOT NULL);
