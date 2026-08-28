package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.resource.service.ReferralNumberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理端资源诊断与订单资源追溯接口。 */
@RestController
@RequestMapping("/api/admin/resources")
public class AdminResourceController {
    private final ReferralNumberService service;

    public AdminResourceController(ReferralNumberService service) {
        this.service = service;
    }

    @GetMapping("/diagnostics")
    public ApiResponse<Map<String, Object>> diagnostics() {
        return ApiResponse.success(service.diagnostics());
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> orderResources(@PathVariable Long orderId) {
        return ApiResponse.success(service.orderResources(orderId));
    }
}
