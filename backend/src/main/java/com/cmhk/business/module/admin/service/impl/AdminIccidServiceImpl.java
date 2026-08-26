package com.cmhk.business.module.admin.service.impl;

import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.entity.IccidAssignmentHistory;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.admin.service.AdminIccidService;
import com.cmhk.business.module.admin.service.IccidService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 复用原 ICCID 业务服务，并为管理端查询增加 Redis 缓存。 */
@Service
public class AdminIccidServiceImpl implements AdminIccidService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(1);

    private final IccidService delegate;
    private final CacheClient cacheClient;
    private final JavaType listType;
    private final JavaType historyType;

    public AdminIccidServiceImpl(IccidService delegate, CacheClient cacheClient, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.cacheClient = cacheClient;
        this.listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, Map.class);
        this.historyType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, IccidAssignmentHistory.class);
    }

    @Override
    public List<Map<String, Object>> list(
            String iccid,
            String batch,
            String status,
            String phone,
            String orderNo) {
        String key = cacheClient.versionedKey(
                AdminCacheKeys.ICCIDS,
                "list:" + AdminCacheKeys.discriminator(iccid, batch, status, phone, orderNo)
        );
        return cacheClient.queryWithMutex(
                key,
                listType,
                () -> delegate.list(iccid, batch, status, phone, orderNo),
                CACHE_TTL,
                EMPTY_CACHE_TTL
        );
    }

    @Override
    public IccidInventory create(String iccid, String batch, String remark, String operator) {
        IccidInventory result = delegate.create(iccid, batch, remark, operator);
        invalidateInventoryCaches();
        return result;
    }

    @Override
    public Map<String, Integer> importFile(MultipartFile file, String batch, String operator) {
        Map<String, Integer> result = delegate.importFile(file, batch, operator);
        invalidateInventoryCaches();
        return result;
    }

    @Override
    public IccidInventory assign(Long id, Long customerId, Long orderId, String reason, String operator) {
        IccidInventory result = delegate.assign(id, customerId, orderId, reason, operator);
        invalidateRelationCaches();
        return result;
    }

    @Override
    public IccidInventory unassign(Long id, String reason, String operator) {
        IccidInventory result = delegate.unassign(id, reason, operator);
        invalidateRelationCaches();
        return result;
    }

    @Override
    public IccidInventory markUsed(Long id, String operator) {
        IccidInventory result = delegate.markUsed(id, operator);
        invalidateRelationCaches();
        return result;
    }

    @Override
    public IccidInventory disable(Long id, String reason, String operator) {
        IccidInventory result = delegate.disable(id, reason, operator);
        invalidateInventoryCaches();
        return result;
    }

    @Override
    public List<IccidAssignmentHistory> history(Long id) {
        String key = cacheClient.versionedKey(AdminCacheKeys.ICCIDS, "history:" + id);
        return cacheClient.queryWithMutex(
                key,
                historyType,
                () -> delegate.history(id),
                CACHE_TTL,
                EMPTY_CACHE_TTL
        );
    }

    private void invalidateInventoryCaches() {
        cacheClient.invalidateNamespacesAfterCommit(AdminCacheKeys.ICCIDS, AdminCacheKeys.DASHBOARD);
    }

    private void invalidateRelationCaches() {
        cacheClient.invalidateNamespacesAfterCommit(
                AdminCacheKeys.ICCIDS,
                AdminCacheKeys.CUSTOMERS,
                AdminCacheKeys.DASHBOARD
        );
    }
}
