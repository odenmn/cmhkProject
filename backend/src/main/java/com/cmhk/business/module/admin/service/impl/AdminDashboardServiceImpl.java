package com.cmhk.business.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.mapper.ReconciliationRowMapper;
import com.cmhk.business.module.admin.mapper.SecondaryCommissionRecordMapper;
import com.cmhk.business.module.admin.service.AdminDashboardService;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.task.entity.OperationTask;
import com.cmhk.business.module.task.mapper.OperationTaskMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端首页统计能力的数据库实现。 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofSeconds(30);

    private final CustomerMapper customerMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final IccidInventoryMapper iccidMapper;
    private final ReconciliationRowMapper reconciliationRowMapper;
    private final SecondaryCommissionRecordMapper commissionRecordMapper;
    private final OperationTaskMapper taskMapper;
    private final CacheClient cacheClient;
    private final JavaType metricsType;

    /** 使用构造器注入统计所需的各业务 Mapper。 */
    public AdminDashboardServiceImpl(CustomerMapper customerMapper, MobilePlanOrderMapper orderMapper,
                                     IccidInventoryMapper iccidMapper, ReconciliationRowMapper reconciliationRowMapper,
                                     SecondaryCommissionRecordMapper commissionRecordMapper,
                                     OperationTaskMapper taskMapper,
                                     CacheClient cacheClient,
                                     ObjectMapper objectMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.iccidMapper = iccidMapper;
        this.reconciliationRowMapper = reconciliationRowMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.taskMapper = taskMapper;
        this.cacheClient = cacheClient;
        this.metricsType = objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, Long.class);
    }

    /** 汇总首页展示的各类待处理和库存指标。 */
    @Override
    public Map<String, Long> metrics() {
        String key = cacheClient.versionedKey(AdminCacheKeys.DASHBOARD, "metrics");
        return cacheClient.queryWithMutex(
                key,
                metricsType,
                this::metricsFromDatabase,
                CACHE_TTL,
                EMPTY_CACHE_TTL
        );
    }

    /** 从数据库实时汇总首页指标，作为缓存未命中或 Redis 异常时的回源逻辑。 */
    private Map<String, Long> metricsFromDatabase() {
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("customers", customerMapper.selectCount(null));
        metrics.put("orders", orderMapper.selectCount(null));
        metrics.put("iccidAvailable", countIccidByStatus("AVAILABLE"));
        metrics.put("iccidAssigned", countIccidByStatus("ASSIGNED"));
        metrics.put("reconciliationExceptions", reconciliationRowMapper.selectCount(
                new LambdaQueryWrapper<ReconciliationRow>().in(ReconciliationRow::getMatchStatus, List.of("UNMATCHED", "AMBIGUOUS"))));
        metrics.put("pendingSettlements", commissionRecordMapper.selectCount(
                new LambdaQueryWrapper<SecondaryCommissionRecord>().eq(SecondaryCommissionRecord::getStatus, "PENDING")));
        metrics.put("pendingTasks", taskMapper.selectCount(new LambdaQueryWrapper<OperationTask>()
                .in(OperationTask::getTaskStatus, List.of("PENDING", "PROCESSING"))));
        return metrics;
    }

    /** 按指定状态统计 ICCID 数量。 */
    private long countIccidByStatus(String status) {
        return iccidMapper.selectCount(new LambdaQueryWrapper<IccidInventory>().eq(IccidInventory::getStatus, status));
    }
}
