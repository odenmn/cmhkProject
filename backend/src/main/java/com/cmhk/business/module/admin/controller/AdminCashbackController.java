package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.cashback.entity.CustomerCashbackInstallment;
import com.cmhk.business.module.cashback.entity.CustomerCashbackPlan;
import com.cmhk.business.module.cashback.entity.CustomerCashbackRule;
import com.cmhk.business.module.cashback.service.CashbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 管理端客户返现规则、计划与期次接口，不接入返现易文件。 */
@RestController
@RequestMapping("/api/admin/cashbacks")
public class AdminCashbackController {

    private static final Logger log = LoggerFactory.getLogger(AdminCashbackController.class);

    private final CashbackService cashbackService;

    public AdminCashbackController(CashbackService cashbackService) {
        this.cashbackService = cashbackService;
    }

    @GetMapping("/rules")
    public ApiResponse<List<CustomerCashbackRule>> rules() {
        List<CustomerCashbackRule> rules = cashbackService.rules();
        log.info("查询返现规则完成，数量={}", rules.size());
        return ApiResponse.success(rules);
    }

    @PostMapping("/rules")
    public ApiResponse<CustomerCashbackRule> createRule(
            @RequestBody CustomerCashbackRule input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(cashbackService.saveRule(null, input, principal));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<CustomerCashbackRule> updateRule(
            @PathVariable Long id,
            @RequestBody CustomerCashbackRule input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(cashbackService.saveRule(id, input, principal));
    }

    @GetMapping("/plans")
    public ApiResponse<List<Map<String, Object>>> plans(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String status,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        List<Map<String, Object>> plans = cashbackService.plans(
                new CashbackService.PlanQuery(customerId, orderId, channelId, status),
                principal);
        log.info("查询返现计划完成，数量={}", plans.size());
        return ApiResponse.success(plans);
    }

    @PostMapping("/plans/orders/{orderId}/generate")
    public ApiResponse<CustomerCashbackPlan> generatePlan(
            @PathVariable Long orderId,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(cashbackService.generateForOrder(orderId, principal));
    }

    /** 管理员按现有订单套餐批量生成返现计划。 */
    @PostMapping("/plans/generate-existing")
    public ApiResponse<Map<String, Object>> generateExistingPlans(
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        Map<String, Object> result = cashbackService.generateForExistingOrders(principal);
        log.info("现有订单返现计划生成完成，生成数={}，冲突数={}", result.get("generated"), result.get("conflictCount"));
        return ApiResponse.success(result);
    }

    @GetMapping("/plans/{id}/installments")
    public ApiResponse<List<CustomerCashbackInstallment>> installments(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(cashbackService.installments(id, principal));
    }

    @PostMapping("/installments/{id}/confirm")
    public ApiResponse<CustomerCashbackInstallment> confirmInstallment(
            @PathVariable Long id,
            @RequestBody ConfirmInstallmentRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(cashbackService.confirmInstallment(id, request.remark(), principal));
    }

    public record ConfirmInstallmentRequest(String remark) {
    }
}
