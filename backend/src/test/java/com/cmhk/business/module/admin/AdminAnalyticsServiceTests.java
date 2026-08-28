package com.cmhk.business.module.admin;

import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminAnalyticsService;
import com.cmhk.business.module.admin.service.impl.AdminAnalyticsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** P6分析参数和渠道数据范围测试。 */
class AdminAnalyticsServiceTests {

    private final AdminAnalyticsService service = new AdminAnalyticsServiceImpl(
            mock(JdbcTemplate.class),
            mock(CacheClient.class),
            new ObjectMapper());

    @Test
    void rejectsReversedDateRange() {
        AdminPrincipal admin = new AdminPrincipal(1L, "admin", "ADMIN", "ALL", null);
        AdminAnalyticsService.AnalyticsQuery query = new AdminAnalyticsService.AnalyticsQuery(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 1),
                null);

        assertThrows(IllegalArgumentException.class, () -> service.analytics(query, admin));
    }

    @Test
    void channelScopeCannotQueryAnotherChannel() {
        AdminPrincipal operator = new AdminPrincipal(2L, "operator", "OPERATOR", "CHANNEL", 4L);
        AdminAnalyticsService.AnalyticsQuery query = new AdminAnalyticsService.AnalyticsQuery(null, null, 3L);

        assertThrows(IllegalArgumentException.class, () -> service.analytics(query, operator));
    }
}
