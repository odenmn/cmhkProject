package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.entity.OperationLog;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminAnalyticsService;
import com.cmhk.business.module.admin.service.AdminDashboardService;
import com.cmhk.business.module.admin.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/** 管理端首页概览和操作日志接口。 */
@RestController
@RequestMapping("/api/admin")
public class AdminOverviewController {

    private static final Logger log = LoggerFactory.getLogger(AdminOverviewController.class);

    private final AdminDashboardService dashboardService;
    private final AdminAnalyticsService analyticsService;
    private final OperationLogService operationLogService;

    /** 使用构造器显式声明依赖，Spring 会自动完成注入。 */
    public AdminOverviewController(
            AdminDashboardService dashboardService,
            AdminAnalyticsService analyticsService,
            OperationLogService operationLogService) {
        this.dashboardService = dashboardService;
        this.analyticsService = analyticsService;
        this.operationLogService = operationLogService;
    }

    /** 返回首页业务指标。 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Long>> dashboard() {
        return ApiResponse.success(dashboardService.metrics());
    }

    /** 返回P6基础分析指标，筛选口径在服务端统一执行。 */
    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long channelId,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        Map<String, Object> result = analyticsService.analytics(
                new AdminAnalyticsService.AnalyticsQuery(startDate, endDate, channelId),
                principal);
        log.info("查询P6基础分析完成，渠道ID={}，开始日期={}，结束日期={}", channelId, startDate, endDate);
        return ApiResponse.success(result);
    }

    /** 按对象类型和操作类型查询最近操作日志。 */
    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> logs(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String operationType) {
        return ApiResponse.success(operationLogService.list(objectType, operationType));
    }
}
