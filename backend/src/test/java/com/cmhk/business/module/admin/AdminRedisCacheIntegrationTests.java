package com.cmhk.business.module.admin;

import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.admin.service.AdminCustomerService;
import com.cmhk.business.module.admin.service.AdminDashboardService;
import com.cmhk.business.module.admin.service.AdminIccidService;
import com.cmhk.business.module.admin.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 管理后台核心查询的真实 Redis 命中与反序列化联调测试。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_REAL_REDIS_TESTS", matches = "true")
class AdminRedisCacheIntegrationTests {
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AdminDashboardService dashboardService;
    @Autowired
    private AdminCustomerService customerService;
    @Autowired
    private AdminOrderService orderService;
    @Autowired
    private AdminIccidService iccidService;

    @Test
    void cachesCoreAdminQueriesInRedis() {
        List<String> keys = cacheKeys();
        redisTemplate.delete(keys);
        try {
            int customerCount = customerService.list(null, null, null).size();
            int orderCount = orderService.list(null, null, null).size();
            int iccidCount = iccidService.list(null, null, null, null, null).size();
            var metrics = dashboardService.metrics();

            for (String key : keys) {
                assertNotNull(redisTemplate.opsForValue().get(key));
            }
            assertEquals(customerCount, customerService.list(null, null, null).size());
            assertEquals(orderCount, orderService.list(null, null, null).size());
            assertEquals(iccidCount, iccidService.list(null, null, null, null, null).size());
            assertEquals(metrics, dashboardService.metrics());
            assertFalse(metrics.isEmpty());
        } finally {
            redisTemplate.delete(keys);
        }
    }

    private List<String> cacheKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(cacheClient.versionedKey(
                AdminCacheKeys.CUSTOMERS,
                "list:" + AdminCacheKeys.discriminator(null, null, null)
        ));
        keys.add(cacheClient.versionedKey(
                AdminCacheKeys.ORDERS,
                "list:" + AdminCacheKeys.discriminator(null, null, null)
        ));
        keys.add(cacheClient.versionedKey(
                AdminCacheKeys.ICCIDS,
                "list:" + AdminCacheKeys.discriminator(null, null, null, null, null)
        ));
        keys.add(cacheClient.versionedKey(AdminCacheKeys.DASHBOARD, "metrics"));
        return keys;
    }
}
