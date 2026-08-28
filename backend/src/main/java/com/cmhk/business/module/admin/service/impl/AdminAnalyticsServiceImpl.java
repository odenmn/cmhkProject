package com.cmhk.business.module.admin.service.impl;

import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminAnalyticsService;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** P6指标数据库实现，筛选和聚合均在服务端完成。 */
@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofSeconds(30);

    private final JdbcTemplate jdbcTemplate;
    private final CacheClient cacheClient;
    private final JavaType analyticsType;

    public AdminAnalyticsServiceImpl(
            JdbcTemplate jdbcTemplate,
            CacheClient cacheClient,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.cacheClient = cacheClient;
        this.analyticsType = objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, Object.class);
    }

    /** 渠道范围账号强制使用自身渠道，不能通过查询参数越权。 */
    @Override
    public Map<String, Object> analytics(AnalyticsQuery query, AdminPrincipal principal) {
        requireInternal(principal);
        validateDateRange(query);
        Long channelId = scopedChannelId(query.channelId(), principal);
        String key = cacheClient.versionedKey(
                AdminCacheKeys.DASHBOARD,
                "analytics:" + AdminCacheKeys.discriminator(
                        query.startDate(),
                        query.endDate(),
                        channelId,
                        principal.roleCode()));
        return cacheClient.queryWithMutex(
                key,
                analyticsType,
                () -> queryDatabase(
                        query.startDate(),
                        query.endDate(),
                        channelId,
                        principal.isAdmin()),
                CACHE_TTL,
                EMPTY_CACHE_TTL);
    }

    private Map<String, Object> queryDatabase(
            LocalDate startDate,
            LocalDate endDate,
            Long channelId,
            boolean amountsVisible) {
        long customers = countCustomers(startDate, endDate, channelId);
        long orders = countOrders(startDate, endDate, channelId, null);
        long onboarded = countOrders(startDate, endDate, channelId, "o.onboard_date IS NOT NULL");
        long activated = countOrders(
                startDate,
                endDate,
                channelId,
                "(o.status IN ('ACTIVATED','COMPLETED') OR o.activation_status = '已激活')");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview(customers, orders, onboarded, activated));
        result.put("operations", operations(startDate, endDate, channelId));
        result.put("resources", resources(channelId));
        result.put("finance", finance(startDate, endDate, channelId, amountsVisible));
        result.put("channelBreakdown", channelBreakdown(startDate, endDate, channelId));
        result.put("definitions", definitions());
        return result;
    }

    private Map<String, Object> overview(long customers, long orders, long onboarded, long activated) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customers", customers);
        result.put("orders", orders);
        result.put("onboarded", onboarded);
        result.put("activated", activated);
        result.put("onboardingRate", percentage(onboarded, orders));
        result.put("activationRate", percentage(activated, orders));
        return result;
    }

    private Map<String, Object> operations(LocalDate startDate, LocalDate endDate, Long channelId) {
        SqlFilter orderFilter = orderFilter(startDate, endDate, channelId);
        SqlFilter taskFilter = datedFilter(
                "t",
                "created_at",
                "COALESCE(t.channel_id, c.channel_id)",
                startDate,
                endDate,
                channelId);
        SqlFilter reconciliationFilter = datedFilter(
                "r",
                "created_at",
                "c.channel_id",
                startDate,
                endDate,
                channelId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingSupplements", count(
                "SELECT COUNT(*) FROM mobile_plan_order o LEFT JOIN customer c ON c.id=o.customer_id "
                        + orderFilter.whereClause()
                        + " AND (o.status='NEED_SUPPLEMENT' OR o.supplement_status IN ('待补件','补件中'))",
                orderFilter.arguments()));
        result.put("pendingTasks", count(
                "SELECT COUNT(*) FROM operation_task t "
                        + "LEFT JOIN mobile_plan_order o ON o.id=t.order_id "
                        + "LEFT JOIN customer c ON c.id=COALESCE(t.customer_id,o.customer_id) "
                        + taskFilter.whereClause()
                        + " AND t.task_status IN ('PENDING','PROCESSING')",
                taskFilter.arguments()));
        result.put("reconciliationExceptions", count(
                "SELECT COUNT(*) FROM cmhk_reconciliation_row r "
                        + "LEFT JOIN mobile_plan_order o ON o.id=r.matched_order_id "
                        + "LEFT JOIN customer c ON c.id=o.customer_id "
                        + reconciliationFilter.whereClause()
                        + " AND r.match_status IN ('UNMATCHED','AMBIGUOUS')",
                reconciliationFilter.arguments()));
        return result;
    }

    private Map<String, Object> resources(Long channelId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availableIccids", countIccids("AVAILABLE", channelId));
        result.put("assignedIccids", countIccids("ASSIGNED", channelId));
        result.put("usedIccids", countIccids("USED", channelId));
        result.put("virtualPendingReplacement", countVirtualPendingReplacement(channelId));
        return result;
    }

    private Map<String, Object> finance(
            LocalDate startDate,
            LocalDate endDate,
            Long channelId,
            boolean amountsVisible) {
        SqlFilter commissionFilter = datedFilter("s", "created_at", "s.channel_id", startDate, endDate, channelId);
        SqlFilter cashbackFilter = datedFilter("p", "generated_at", "p.channel_id", startDate, endDate, channelId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("commissionRecords", count(
                "SELECT COUNT(*) FROM secondary_commission_record s " + commissionFilter.whereClause(),
                commissionFilter.arguments()));
        result.put("pendingSettlements", count(
                "SELECT COUNT(*) FROM secondary_commission_record s "
                        + commissionFilter.whereClause()
                        + " AND s.status='PENDING'",
                commissionFilter.arguments()));
        result.put("cashbackPlans", count(
                "SELECT COUNT(*) FROM customer_cashback_plan p " + cashbackFilter.whereClause(),
                cashbackFilter.arguments()));
        result.put("pendingActivationCashbacks", count(
                "SELECT COUNT(*) FROM customer_cashback_plan p "
                        + cashbackFilter.whereClause()
                        + " AND p.status='PENDING_ACTIVATION'",
                cashbackFilter.arguments()));
        result.put("amountsVisible", amountsVisible);
        result.put("commissionAmount", amountsVisible
                ? amount(
                        "SELECT COALESCE(SUM(s.final_amount),0) FROM secondary_commission_record s "
                                + commissionFilter.whereClause(),
                        commissionFilter.arguments())
                : null);
        result.put("cashbackAmount", amountsVisible
                ? amount(
                        "SELECT COALESCE(SUM(p.total_amount),0) FROM customer_cashback_plan p "
                                + cashbackFilter.whereClause(),
                        cashbackFilter.arguments())
                : null);
        return result;
    }

    private List<Map<String, Object>> channelBreakdown(
            LocalDate startDate,
            LocalDate endDate,
            Long selectedChannelId) {
        String sql = "SELECT id,channel_name FROM channel WHERE enabled=1"
                + (selectedChannelId == null ? "" : " AND id=?")
                + " ORDER BY channel_name";
        List<Map<String, Object>> channels = selectedChannelId == null
                ? jdbcTemplate.queryForList(sql)
                : jdbcTemplate.queryForList(sql, selectedChannelId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> channel : channels) {
            Long channelId = ((Number) channel.get("id")).longValue();
            long customers = countCustomers(startDate, endDate, channelId);
            long orders = countOrders(startDate, endDate, channelId, null);
            long onboarded = countOrders(startDate, endDate, channelId, "o.onboard_date IS NOT NULL");
            long activated = countOrders(
                    startDate,
                    endDate,
                    channelId,
                    "(o.status IN ('ACTIVATED','COMPLETED') OR o.activation_status='已激活')");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("channelId", channelId);
            row.put("channelName", channel.get("channel_name"));
            row.putAll(overview(customers, orders, onboarded, activated));
            rows.add(row);
        }
        return rows;
    }

    private long countCustomers(LocalDate startDate, LocalDate endDate, Long channelId) {
        SqlFilter filter = datedFilter("c", "created_at", "c.channel_id", startDate, endDate, channelId);
        return count("SELECT COUNT(*) FROM customer c " + filter.whereClause(), filter.arguments());
    }

    private long countOrders(
            LocalDate startDate,
            LocalDate endDate,
            Long channelId,
            String extraCondition) {
        SqlFilter filter = orderFilter(startDate, endDate, channelId);
        String sql = "SELECT COUNT(*) FROM mobile_plan_order o LEFT JOIN customer c ON c.id=o.customer_id "
                + filter.whereClause()
                + (extraCondition == null ? "" : " AND " + extraCondition);
        return count(sql, filter.arguments());
    }

    private SqlFilter orderFilter(LocalDate startDate, LocalDate endDate, Long channelId) {
        return datedFilter("o", "created_at", "c.channel_id", startDate, endDate, channelId);
    }

    private long countIccids(String status, Long channelId) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(status);
        String sql = "SELECT COUNT(*) FROM iccid_inventory i LEFT JOIN customer c ON c.id=i.current_customer_id "
                + "WHERE i.status=?";
        if (channelId != null) {
            sql += " AND c.channel_id=?";
            arguments.add(channelId);
        }
        return count(sql, arguments);
    }

    private long countVirtualPendingReplacement(Long channelId) {
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM iccid_inventory i LEFT JOIN customer c ON c.id=i.current_customer_id "
                + "WHERE i.card_type='VIRTUAL' AND i.status<>'REPLACED'";
        if (channelId != null) {
            sql += " AND c.channel_id=?";
            arguments.add(channelId);
        }
        return count(sql, arguments);
    }

    private SqlFilter datedFilter(
            String alias,
            String dateColumn,
            String channelColumn,
            LocalDate startDate,
            LocalDate endDate,
            Long channelId) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> arguments = new ArrayList<>();
        if (startDate != null) {
            where.append(" AND ").append(alias).append('.').append(dateColumn).append(">=?");
            arguments.add(Timestamp.valueOf(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            where.append(" AND ").append(alias).append('.').append(dateColumn).append("<?");
            arguments.add(Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
        }
        if (channelId != null) {
            where.append(" AND ").append(channelColumn).append("=?");
            arguments.add(channelId);
        }
        return new SqlFilter(where.toString(), arguments);
    }

    private long count(String sql, List<Object> arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments.toArray());
        return value == null ? 0L : value;
    }

    private BigDecimal amount(String sql, List<Object> arguments) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, arguments.toArray());
        return value == null
                ? BigDecimal.ZERO.setScale(2)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /** 指标定义集中返回，避免页面和查询代码出现不同口径。 */
    private Map<String, String> definitions() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("customers", "客户创建时间命中筛选范围的客户数");
        result.put("orders", "订单创建时间命中筛选范围的订单数");
        result.put("onboarded", "筛选订单中上台日期不为空的订单数");
        result.put("activated", "标准状态为已激活/已完成，或激活状态为已激活的订单数");
        result.put("onboardingRate", "上台订单数 ÷ 订单数");
        result.put("activationRate", "激活订单数 ÷ 订单数");
        result.put("inventory", "ICCID库存为查询时点快照，不受时间范围影响");
        result.put("amounts", "佣金和返现金额只向管理员展示");
        return result;
    }

    private Long scopedChannelId(Long requestedChannelId, AdminPrincipal principal) {
        if (!"CHANNEL".equals(principal.scopeType())) {
            return requestedChannelId;
        }
        if (principal.scopeId() == null) {
            throw new IllegalArgumentException("当前账号缺少渠道数据范围");
        }
        if (requestedChannelId != null && !requestedChannelId.equals(principal.scopeId())) {
            throw new IllegalArgumentException("不能查询其他渠道的数据");
        }
        return principal.scopeId();
    }

    private void validateDateRange(AnalyticsQuery query) {
        if (query.startDate() != null
                && query.endDate() != null
                && query.startDate().isAfter(query.endDate())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private void requireInternal(AdminPrincipal principal) {
        if (principal == null || !principal.isInternalOperator()) {
            throw new IllegalArgumentException("当前账号无数据分析权限");
        }
    }

    private record SqlFilter(String whereClause, List<Object> arguments) {
    }
}
