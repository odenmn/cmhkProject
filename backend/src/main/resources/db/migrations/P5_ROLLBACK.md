# P5-A 回滚说明

P5-A 仅新增订单实际激活时间字段、返现规则表、返现计划表和返现期次表。

补充迁移V006仅允许待激活返现计划的 `activated_at` 为空。若表内已经存在待激活计划，不得直接将该字段恢复为 `NOT NULL`。

## 未产生返现计划数据时

确认已备份后，可停止使用 P5-A 应用代码，并按依赖顺序删除新增表和订单索引/字段。

```sql
DROP TABLE customer_cashback_installment;
DROP TABLE customer_cashback_plan;
DROP TABLE customer_cashback_rule;
ALTER TABLE mobile_plan_order
    DROP INDEX idx_mobile_plan_order_activated_at,
    DROP COLUMN activated_at;
```

## 已产生返现计划数据时

不得直接删除表或字段。应先备份三张返现表，再将应用回退为只读兼容；返现计划和人工确认记录属于业务事实，需另行确认后才能清理。
