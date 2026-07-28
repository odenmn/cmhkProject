package com.cmhk.business.module.mobile.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.mobile.dto.MobilePlanOrderCreateRequest;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.service.MobilePlanOrderService;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile-plans")
public class MobilePlanController {

    private static final Logger log = LoggerFactory.getLogger(MobilePlanController.class);

    private final MobilePlanService mobilePlanService;
    private final MobilePlanOrderService mobilePlanOrderService;

    public MobilePlanController(MobilePlanService mobilePlanService, MobilePlanOrderService mobilePlanOrderService) {
        this.mobilePlanService = mobilePlanService;
        this.mobilePlanOrderService = mobilePlanOrderService;
    }

    @GetMapping
    public ApiResponse<List<MobilePlan>> listPlans() {
        log.info("开始查询启用移动套餐");
        List<MobilePlan> plans = mobilePlanService.listEnabledPlansWithOffers();
        int offerCount = plans.stream()
                .mapToInt(plan -> plan.getOffers() == null ? 0 : plan.getOffers().size())
                .sum();
        log.info("查询启用移动套餐完成，套餐数量={}，优惠权益数量={}", plans.size(), offerCount);
        return ApiResponse.success(plans);
    }

    @PostMapping("/orders")
    public ApiResponse<MobilePlanOrder> createOrder(@Valid @RequestBody MobilePlanOrderCreateRequest request) {
        log.info("开始创建移动套餐转人工订单，planCode={}", request.getPlanCode());
        MobilePlanOrder order = mobilePlanOrderService.createTransferOrder(request);
        log.info("移动套餐转人工订单创建完成，orderNo={}，planCode={}，status={}",
                order.getOrderNo(), order.getPlanCode(), order.getStatus());
        return ApiResponse.success(order);
    }
}
