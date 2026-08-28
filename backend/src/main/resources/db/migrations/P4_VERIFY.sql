SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('operation_task', 'operation_task_history')
ORDER BY table_name;
SELECT task_status, COUNT(*) AS row_count
FROM operation_task
GROUP BY task_status
ORDER BY task_status;
SELECT open_dedup_key, COUNT(*) AS duplicate_count
FROM operation_task
WHERE open_dedup_key IS NOT NULL
GROUP BY open_dedup_key
HAVING COUNT(*) > 1;
SELECT history.task_id, COUNT(*) AS history_count
FROM operation_task_history history
GROUP BY history.task_id
ORDER BY history.task_id;
