# P1回滚与兼容说明

P1不删除旧渠道表，也不清空真实数据。出现问题时优先回滚应用版本，并保持新增字段和映射表不动。

## 应用回滚

1. 停止当前后端和管理端。
2. 将应用代码切回P0版本。
3. P0代码仍读取`secondary_channel`，旧表在P1中保持完整，因此渠道档案兼容读取仍可恢复。

## 佣金渠道ID回退

只有确认必须回到P0应用时，才执行以下回填。执行前必须备份`secondary_commission_record`：

```sql
UPDATE secondary_commission_record record
INNER JOIN channel_legacy_mapping mapping
        ON mapping.legacy_table = 'secondary_channel'
       AND mapping.channel_id = record.channel_id
       AND mapping.migration_status = 'MIGRATED'
SET record.channel_id = mapping.legacy_id;
```

## 结构处理

- 不建议立即删除`channel`新增字段、`admin_user`范围字段、映射表和异常表。
- 新字段不会妨碍P0代码运行，保留它们比直接删列更安全。
- `customer.channel_id`的回填来自唯一的`customer_channel_binding`业务事实，不自动反向清空。
- 如果确需删除新增结构，必须先导出映射表、异常表以及两个受影响主表的完整备份，并另行取得确认。
