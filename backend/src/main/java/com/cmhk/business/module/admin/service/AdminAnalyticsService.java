package com.cmhk.business.module.admin.service;

import com.cmhk.business.module.admin.security.AdminPrincipal;

import java.time.LocalDate;
import java.util.Map;

/** P6管理端基础数据分析服务。 */
public interface AdminAnalyticsService {

    Map<String, Object> analytics(AnalyticsQuery query, AdminPrincipal principal);

    record AnalyticsQuery(LocalDate startDate, LocalDate endDate, Long channelId) {
    }
}
