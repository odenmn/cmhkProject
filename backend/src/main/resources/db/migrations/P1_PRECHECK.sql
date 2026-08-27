-- P1执行前只读检查：统一渠道与管理员权限。

SHOW CREATE TABLE channel;
SHOW CREATE TABLE secondary_channel;
SHOW CREATE TABLE secondary_commission_record;
SHOW CREATE TABLE customer;
SHOW CREATE TABLE customer_channel_binding;
SHOW CREATE TABLE admin_user;

SELECT 'channel' AS table_name, COUNT(*) AS row_count FROM channel
UNION ALL SELECT 'secondary_channel', COUNT(*) FROM secondary_channel
UNION ALL SELECT 'secondary_commission_record', COUNT(*) FROM secondary_commission_record
UNION ALL SELECT 'customer', COUNT(*) FROM customer
UNION ALL SELECT 'customer_channel_binding', COUNT(*) FROM customer_channel_binding
UNION ALL SELECT 'admin_user', COUNT(*) FROM admin_user;

SELECT channel_code, COUNT(*) AS duplicate_count
FROM channel
GROUP BY channel_code
HAVING COUNT(*) > 1;

SELECT channel_code, COUNT(*) AS duplicate_count
FROM secondary_channel
GROUP BY channel_code
HAVING COUNT(*) > 1;

SELECT legacy.id AS legacy_id,
       legacy.channel_code,
       unified.id AS unified_channel_id
FROM secondary_channel legacy
INNER JOIN channel unified ON unified.channel_code = legacy.channel_code;

SELECT customer_record.id AS customer_id,
       customer_record.channel_id AS customer_channel_id,
       binding.channel_id AS binding_channel_id
FROM customer customer_record
INNER JOIN customer_channel_binding binding ON binding.customer_id = customer_record.id
WHERE customer_record.channel_id IS NULL
   OR customer_record.channel_id <> binding.channel_id;

SELECT record.id AS commission_record_id,
       record.channel_id AS legacy_channel_id
FROM secondary_commission_record record
LEFT JOIN secondary_channel legacy ON legacy.id = record.channel_id
WHERE legacy.id IS NULL;

SELECT role_code, status, COUNT(*) AS row_count
FROM admin_user
GROUP BY role_code, status;
