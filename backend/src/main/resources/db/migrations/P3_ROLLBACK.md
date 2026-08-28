# P3 回滚说明

1. 回滚前停止 P3 资源写操作并备份三张推荐号码表。
2. 应用回滚到 P2 版本后，依次删除 `referral_number_assignment_history`、`referral_number_pool`、`referral_chain`。
3. `iccid_assignment_history` 中 `action_type='MIGRATION_BASELINE'` 且 `operator_name='P3_MIGRATION'` 的记录只是补充历史，可按需删除；删除不会改变卡池状态。
4. P3 不修改既有客户、订单、ICCID 当前数据，因此无需反向更新这些业务表。
5. 生产回滚必须先核对备份，禁止清空真实业务数据。
