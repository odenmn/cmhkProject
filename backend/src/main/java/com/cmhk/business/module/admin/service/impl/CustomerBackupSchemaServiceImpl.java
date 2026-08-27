package com.cmhk.business.module.admin.service.impl;

import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.admin.service.CustomerBackupSchemaService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** 客户备份导入数据库结构准备实现。 */
@Service
public class CustomerBackupSchemaServiceImpl implements CustomerBackupSchemaService {

    private static final String BACKUP_SOURCE = "CMHK_BACKUP";

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final CacheClient cacheClient;

    public CustomerBackupSchemaServiceImpl(
            JdbcTemplate jdbcTemplate,
            OperationLogService operationLogService,
            CacheClient cacheClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.operationLogService = operationLogService;
        this.cacheClient = cacheClient;
    }

    /** 仅执行幂等 DDL，不读取或改写客户业务数据。 */
    @Override
    public synchronized void ensureReady() {
        makeHistoricalFieldsNullable();
        ensureSourceColumns();
        normalizeCustomerModel();
        ensureIccidLifecycleColumns();
        ensureUniqueIndex("customer", "uk_customer_source", "(source_system, source_customer_id)");
        ensureUniqueIndex("mobile_plan_order", "uk_order_source_record", "(order_source, source_record_id)");
        ensureAuditTables();
    }

    private void makeHistoricalFieldsNullable() {
        jdbcTemplate.execute("ALTER TABLE customer MODIFY phone VARCHAR(32) NULL");
        jdbcTemplate.execute("ALTER TABLE customer MODIFY phone_verified_at DATETIME NULL");
        jdbcTemplate.execute("ALTER TABLE mobile_plan_order MODIFY plan_code VARCHAR(64) NULL");
        jdbcTemplate.execute("ALTER TABLE mobile_plan_order MODIFY plan_name VARCHAR(128) NULL");
        jdbcTemplate.execute("ALTER TABLE mobile_plan_order MODIFY monthly_fee DECIMAL(10, 2) NULL");
        jdbcTemplate.execute("ALTER TABLE mobile_plan_order MODIFY contact_phone VARCHAR(32) NULL");
    }

    private void ensureSourceColumns() {
        ensureColumn("customer", "customer_category", "VARCHAR(32) NULL COMMENT '业务客户类别，例如留学生、地产客户、研究生'");
        ensureColumn("customer", "source_system", "VARCHAR(32) NULL");
        ensureColumn("customer", "source_customer_id", "VARCHAR(64) NULL");
        ensureColumn("mobile_plan_order", "source_record_id", "VARCHAR(64) NULL");
        ensureColumn("mobile_plan_order", "source_channel_name", "VARCHAR(128) NULL");
        ensureColumn("mobile_plan_order", "umall_status", "VARCHAR(32) NULL");
        ensureColumn("mobile_plan_order", "onboard_date", "DATE NULL");
    }

    /** 将历史摘要中的客户类别拆出，并把客户状态转换为稳定数字码。 */
    @Override
    public synchronized Map<String, Integer> normalizeCustomerModel() {
        ensureColumn("customer", "customer_category", "VARCHAR(32) NULL COMMENT '业务客户类别，例如留学生、地产客户、研究生'");
        jdbcTemplate.update("UPDATE customer SET customer_category = TRIM(SUBSTRING(requirement_summary, CHAR_LENGTH('来源客户类型：') + 1)) WHERE source_system = 'CMHK_BACKUP' AND customer_category IS NULL AND requirement_summary LIKE '来源客户类型：%'");
        jdbcTemplate.update("UPDATE customer SET requirement_summary = NULL WHERE source_system = 'CMHK_BACKUP' AND requirement_summary LIKE '来源客户类型：%'");
        jdbcTemplate.update("UPDATE mobile_plan_order SET activation_status = CASE WHEN status LIKE '%已激活%' THEN '已激活' WHEN onboard_date IS NOT NULL OR status LIKE '%待激活%' THEN '待激活' ELSE NULL END WHERE order_source = 'CMHK_BACKUP'");
        if (!isNumericCustomerStatus()) {
            migrateTextCustomerStatus();
        }
        jdbcTemplate.execute("ALTER TABLE customer MODIFY current_status TINYINT NOT NULL DEFAULT 0 COMMENT '客户状态码：0待处理，1跟进中，2待资料，3办理中，4待激活，5已激活，6已完成，9无效'");

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("backupCustomers", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP'"));
        result.put("categorizedCustomers", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP' AND customer_category IS NOT NULL"));
        result.put("occupiedRequirementSummaries", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP' AND requirement_summary LIKE '来源客户类型：%'"));
        result.put("onboardedCustomers", count("SELECT COUNT(DISTINCT c.id) FROM customer c JOIN mobile_plan_order o ON o.customer_id = c.id WHERE c.source_system = 'CMHK_BACKUP' AND o.order_source = 'CMHK_BACKUP' AND o.onboard_date IS NOT NULL"));
        result.put("waitingActivationCustomers", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP' AND current_status = 4"));
        result.put("activatedCustomers", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP' AND current_status = 5"));
        result.put("legacyOnboardActivationStatuses", count("SELECT COUNT(*) FROM mobile_plan_order WHERE order_source = 'CMHK_BACKUP' AND activation_status = '已上台'"));
        return result;
    }

    /** 只读统计当前不应保留在订单表中的历史模拟订单及关联风险。 */
    @Override
    public Map<String, Integer> previewOrderScope() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("backupCustomers", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP'"));
        result.put("onboardedCustomers", count("SELECT COUNT(*) FROM customer WHERE source_system = 'CMHK_BACKUP' AND current_status IN (4, 5, 6)"));
        result.put("currentBackupOrders", count("SELECT COUNT(*) FROM mobile_plan_order WHERE order_source = 'CMHK_BACKUP'"));
        result.put("ordersToRemove", count(outOfScopeOrderCountSql()));
        result.put("iccidBindingConflicts", count("SELECT COUNT(*) FROM iccid_inventory i JOIN mobile_plan_order o ON i.current_order_id = o.id JOIN customer c ON o.customer_id = c.id WHERE o.order_source = 'CMHK_BACKUP' AND c.source_system = 'CMHK_BACKUP' AND c.current_status NOT IN (4, 5, 6)"));
        result.put("reconciliationConflicts", count("SELECT COUNT(*) FROM cmhk_reconciliation_row r JOIN mobile_plan_order o ON r.matched_order_id = o.id JOIN customer c ON o.customer_id = c.id WHERE o.order_source = 'CMHK_BACKUP' AND c.source_system = 'CMHK_BACKUP' AND c.current_status NOT IN (4, 5, 6)"));
        result.put("settlementConflicts", count("SELECT COUNT(*) FROM secondary_commission_record s JOIN mobile_plan_order o ON s.order_id = o.id JOIN customer c ON o.customer_id = c.id WHERE o.order_source = 'CMHK_BACKUP' AND c.source_system = 'CMHK_BACKUP' AND c.current_status NOT IN (4, 5, 6)"));
        return result;
    }

    /**
     * 确认清理不符合上台状态口径的历史模拟订单。
     *
     * <p>存在 ICCID、对账或结算关联时拒绝执行，避免自动破坏历史关系。</p>
     */
    @Override
    @Transactional
    public synchronized Map<String, Integer> confirmOrderScope(String operator) {
        Map<String, Integer> result = previewOrderScope();
        int relatedRecords = result.get("iccidBindingConflicts")
                + result.get("reconciliationConflicts")
                + result.get("settlementConflicts");
        if (relatedRecords > 0) {
            throw new IllegalStateException("存在已关联的 ICCID、对账或结算记录，不能自动清理历史订单");
        }

        int importRowsCleared = jdbcTemplate.update("UPDATE customer_backup_import_row r JOIN mobile_plan_order o ON r.order_id = o.id JOIN customer c ON o.customer_id = c.id SET r.order_id = NULL WHERE o.order_source = 'CMHK_BACKUP' AND c.source_system = 'CMHK_BACKUP' AND c.current_status NOT IN (4, 5, 6)");
        int ordersRemoved = jdbcTemplate.update("DELETE o FROM mobile_plan_order o JOIN customer c ON o.customer_id = c.id WHERE o.order_source = 'CMHK_BACKUP' AND c.source_system = 'CMHK_BACKUP' AND c.current_status NOT IN (4, 5, 6)");
        result.put("importRowsCleared", importRowsCleared);
        result.put("ordersRemoved", ordersRemoved);
        operationLogService.record(
                operator,
                "CUSTOMER_BACKUP_ORDER_SCOPE_CORRECT",
                "CUSTOMER_BACKUP",
                null,
                null,
                result,
                "仅保留状态为待激活、已激活、已完成的模拟订单"
        );
        cacheClient.invalidateNamespacesAfterCommit(
                AdminCacheKeys.CUSTOMERS,
                AdminCacheKeys.ORDERS,
                AdminCacheKeys.ICCIDS,
                AdminCacheKeys.DASHBOARD
        );
        return result;
    }

    private String outOfScopeOrderCountSql() {
        return "SELECT COUNT(*) FROM mobile_plan_order o JOIN customer c ON o.customer_id = c.id "
                + "WHERE o.order_source = 'CMHK_BACKUP' AND c.source_system = 'CMHK_BACKUP' "
                + "AND c.current_status NOT IN (4, 5, 6)";
    }

    private boolean isNumericCustomerStatus() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'customer' AND column_name = 'current_status'",
                String.class
        );
        return "tinyint".equalsIgnoreCase(dataType)
                || "smallint".equalsIgnoreCase(dataType)
                || "int".equalsIgnoreCase(dataType)
                || "bigint".equalsIgnoreCase(dataType);
    }

    private void migrateTextCustomerStatus() {
        jdbcTemplate.update("UPDATE customer c SET current_status = CASE WHEN current_status LIKE '%无效%' THEN '9' WHEN current_status LIKE '%完成%' THEN '6' WHEN current_status LIKE '%已激活%' THEN '5' WHEN EXISTS (SELECT 1 FROM mobile_plan_order o WHERE o.customer_id = c.id AND o.order_source = 'CMHK_BACKUP' AND o.onboard_date IS NOT NULL) THEN '4' WHEN current_status LIKE '%资料%' THEN '2' WHEN current_status LIKE '%待激活%' THEN '4' WHEN current_status LIKE '%寄出%' OR current_status LIKE '%办理%' THEN '3' WHEN current_status IS NULL OR current_status = '' OR current_status = '待处理' THEN '0' ELSE '1' END");
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private void ensureIccidLifecycleColumns() {
        ensureColumn("iccid_inventory", "card_type", "VARCHAR(16) NOT NULL DEFAULT 'REAL'");
        ensureColumn("iccid_inventory", "service_number", "VARCHAR(32) NULL");
        ensureColumn("iccid_inventory", "source_system", "VARCHAR(32) NULL");
        ensureColumn("iccid_inventory", "source_record_id", "VARCHAR(64) NULL");
        ensureColumn("iccid_inventory", "replaced_by_iccid_id", "BIGINT NULL");
        ensureColumn("iccid_inventory", "replaced_at", "DATETIME NULL");
    }

    private void ensureAuditTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS customer_backup_import (id BIGINT PRIMARY KEY AUTO_INCREMENT, file_name VARCHAR(255) NOT NULL, file_hash CHAR(64) NOT NULL, status VARCHAR(24) NOT NULL, total_count INT NOT NULL DEFAULT 0, customer_count INT NOT NULL DEFAULT 0, order_count INT NOT NULL DEFAULT 0, iccid_count INT NOT NULL DEFAULT 0, exception_count INT NOT NULL DEFAULT 0, operator_name VARCHAR(64) NOT NULL, confirmed_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY uk_customer_backup_file_hash (file_hash)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS customer_backup_import_row (id BIGINT PRIMARY KEY AUTO_INCREMENT, import_id BIGINT NOT NULL, source_row_number INT NOT NULL, source_id VARCHAR(64), customer_id BIGINT, order_id BIGINT, iccid_id BIGINT, result_status VARCHAR(24) NOT NULL, exception_code VARCHAR(64), exception_reason VARCHAR(512), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX idx_customer_backup_row_import (import_id), INDEX idx_customer_backup_row_source (source_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void ensureUniqueIndex(String tableName, String indexName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD UNIQUE INDEX " + indexName + " " + definition);
        }
    }
}
