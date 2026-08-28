package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.mobile.entity.ChannelProductPolicy;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOffer;
import com.cmhk.business.module.mobile.service.AdminProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** 管理端产品运营接口。 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    private final AdminProductService service;

    public AdminProductController(AdminProductService service) {
        this.service = service;
    }

    @GetMapping("/plans")
    public ApiResponse<List<MobilePlan>> plans() {
        List<MobilePlan> rows = service.listPlans();
        log.info("查询管理端套餐完成，数量={}", rows.size());
        return ApiResponse.success(rows);
    }

    @PostMapping("/plans")
    public ApiResponse<MobilePlan> createPlan(
            @RequestBody MobilePlan input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.savePlan(null, input, principal.username()));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<MobilePlan> updatePlan(
            @PathVariable Long id,
            @RequestBody MobilePlan input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.savePlan(id, input, principal.username()));
    }

    @DeleteMapping("/plans/{id}")
    public ApiResponse<Void> disablePlan(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        service.disablePlan(id, principal.username());
        return ApiResponse.success(null);
    }

    @GetMapping("/offers")
    public ApiResponse<List<MobilePlanOffer>> offers(@RequestParam String planCode) {
        return ApiResponse.success(service.listOffers(planCode));
    }

    @PostMapping("/offers")
    public ApiResponse<MobilePlanOffer> createOffer(
            @RequestBody MobilePlanOffer input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.saveOffer(null, input, principal.username()));
    }

    @PutMapping("/offers/{id}")
    public ApiResponse<MobilePlanOffer> updateOffer(
            @PathVariable Long id,
            @RequestBody MobilePlanOffer input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.saveOffer(id, input, principal.username()));
    }

    @DeleteMapping("/offers/{id}")
    public ApiResponse<Void> deleteOffer(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        service.deleteOffer(id, principal.username());
        return ApiResponse.success(null);
    }

    @GetMapping("/policies")
    public ApiResponse<List<Map<String, Object>>> policies() {
        return ApiResponse.success(service.listPolicies());
    }

    @PostMapping("/policies")
    public ApiResponse<ChannelProductPolicy> createPolicy(
            @RequestBody ChannelProductPolicy input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.savePolicy(null, input, principal.username()));
    }

    @PutMapping("/policies/{id}")
    public ApiResponse<ChannelProductPolicy> updatePolicy(
            @PathVariable Long id,
            @RequestBody ChannelProductPolicy input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.savePolicy(id, input, principal.username()));
    }

    @DeleteMapping("/policies/{id}")
    public ApiResponse<Void> deletePolicy(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        service.deletePolicy(id, principal.username());
        return ApiResponse.success(null);
    }
}
