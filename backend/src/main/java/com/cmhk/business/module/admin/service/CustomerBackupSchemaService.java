package com.cmhk.business.module.admin.service;

import java.util.Map;

/** 客户备份导入所需数据库结构准备服务。 */
public interface CustomerBackupSchemaService {

    /** 以幂等方式补齐导入所需字段、索引和留痕表。 */
    void ensureReady();

    /** 将旧客户类型摘要和文本状态迁移到独立字段及数字状态码。 */
    Map<String, Integer> normalizeCustomerModel();

    /** 只读预览不符合当前上台口径的历史模拟订单。 */
    Map<String, Integer> previewOrderScope();

    /** 确认清理未上台客户的历史模拟订单，并保留客户和可用 ICCID。 */
    Map<String, Integer> confirmOrderScope(String operator);
}
