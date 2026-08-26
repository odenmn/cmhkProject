package com.cmhk.business.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.mapper.ReconciliationRowMapper;
import com.cmhk.business.module.admin.mapper.SecondaryCommissionRecordMapper;
import com.cmhk.business.module.admin.service.AdminDashboardService;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端首页统计能力的数据库实现。 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final CustomerMapper customerMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final IccidInventoryMapper iccidMapper;
    private final ReconciliationRowMapper reconciliationRowMapper;
    private final SecondaryCommissionRecordMapper commissionRecordMapper;

    /** 使用构造器注入统计所需的各业务 Mapper。 */
    public AdminDashboardServiceImpl(CustomerMapper customerMapper, MobilePlanOrderMapper orderMapper,
                                     IccidInventoryMapper iccidMapper, ReconciliationRowMapper reconciliationRowMapper,
                                     SecondaryCommissionRecordMapper commissionRecordMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.iccidMapper = iccidMapper;
        this.reconciliationRowMapper = reconciliationRowMapper;
        this.commissionRecordMapper = commissionRecordMapper;
    }

    /** 汇总首页展示的各类待处理和库存指标。 */
    @Override
    public Map<String, Long> metrics() {
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("customers", customerMapper.selectCount(null));
        metrics.put("orders", orderMapper.selectCount(null));
        metrics.put("iccidAvailable", countIccidByStatus("AVAILABLE"));
        metrics.put("iccidAssigned", countIccidByStatus("ASSIGNED"));
        metrics.put("reconciliationExceptions", reconciliationRowMapper.selectCount(
                new LambdaQueryWrapper<ReconciliationRow>().in(ReconciliationRow::getMatchStatus, List.of("UNMATCHED", "AMBIGUOUS"))));
        metrics.put("pendingSettlements", commissionRecordMapper.selectCount(
                new LambdaQueryWrapper<SecondaryCommissionRecord>().eq(SecondaryCommissionRecord::getStatus, "PENDING")));
        return metrics;
    }

    /** 按指定状态统计 ICCID 数量。 */
    private long countIccidByStatus(String status) {
        return iccidMapper.selectCount(new LambdaQueryWrapper<IccidInventory>().eq(IccidInventory::getStatus, status));
    }
}
