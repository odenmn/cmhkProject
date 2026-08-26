package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.admin.entity.OperationLog;
import com.cmhk.business.module.admin.service.AdminDashboardService;
import com.cmhk.business.module.admin.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 管理端首页概览和操作日志接口。 */
@RestController
@RequestMapping("/api/admin")
public class AdminOverviewController {

    private final AdminDashboardService dashboardService;
    private final OperationLogService operationLogService;

    /** 使用构造器显式声明依赖，Spring 会自动完成注入。 */
    public AdminOverviewController(
            AdminDashboardService dashboardService,
            OperationLogService operationLogService) {
        this.dashboardService = dashboardService;
        this.operationLogService = operationLogService;
    }

    /** 返回首页业务指标。 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Long>> dashboard() {
        return ApiResponse.success(dashboardService.metrics());
    }

    /** 按对象类型和操作类型查询最近操作日志。 */
    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> logs(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String operationType) {
        return ApiResponse.success(operationLogService.list(objectType, operationType));
    }
}
