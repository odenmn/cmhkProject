package com.cmhk.business.module.mobile.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.mobile.dto.MobilePlanOrderCreateRequest;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.service.MobilePlanOrderService;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile-plans")
public class MobilePlanController {

    private final MobilePlanService mobilePlanService;
    private final MobilePlanOrderService mobilePlanOrderService;

    public MobilePlanController(MobilePlanService mobilePlanService, MobilePlanOrderService mobilePlanOrderService) {
        this.mobilePlanService = mobilePlanService;
        this.mobilePlanOrderService = mobilePlanOrderService;
    }

    @GetMapping
    public ApiResponse<List<MobilePlan>> listPlans() {
        return ApiResponse.success(
                mobilePlanService.lambdaQuery()
                        .eq(MobilePlan::getEnabled, 1)
                        .orderByAsc(MobilePlan::getSortOrder)
                        .list()
        );
    }

    @PostMapping("/orders")
    public ApiResponse<MobilePlanOrder> createOrder(@Valid @RequestBody MobilePlanOrderCreateRequest request) {
        return ApiResponse.success(mobilePlanOrderService.createTransferOrder(request));
    }
}

