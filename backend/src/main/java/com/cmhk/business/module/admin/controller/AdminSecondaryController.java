package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.entity.SecondaryChannel;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRule;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.SecondarySettlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** 渠道佣金及旧二级渠道只读兼容接口。 */
@RestController
@RequestMapping("/api/admin/secondary")
public class AdminSecondaryController {

    private final SecondarySettlementService service;

    public AdminSecondaryController(SecondarySettlementService service) {
        this.service = service;
    }

    /** 旧二级渠道仅用于迁移核查，新页面不再调用。 */
    @GetMapping("/channels")
    public ApiResponse<List<SecondaryChannel>> legacyChannels() {
        return ApiResponse.success(service.channels());
    }

    /** 明确停用旧渠道写入口，避免再次产生双主档。 */
    @PostMapping("/channels")
    public ApiResponse<SecondaryChannel> createLegacyChannel(
            @RequestBody SecondaryChannel input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.saveChannel(null, input, principal.username()));
    }

    /** 明确停用旧渠道写入口，避免再次产生双主档。 */
    @PutMapping("/channels/{id}")
    public ApiResponse<SecondaryChannel> updateLegacyChannel(
            @PathVariable Long id,
            @RequestBody SecondaryChannel input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.saveChannel(id, input, principal.username()));
    }

    @GetMapping("/rules")
    public ApiResponse<List<SecondaryCommissionRule>> rules() {
        return ApiResponse.success(service.rules());
    }

    @PostMapping("/rules")
    public ApiResponse<SecondaryCommissionRule> createRule(
            @RequestBody SecondaryCommissionRule input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.saveRule(null, input, principal));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<SecondaryCommissionRule> updateRule(
            @PathVariable Long id,
            @RequestBody SecondaryCommissionRule input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.saveRule(id, input, principal));
    }

    @GetMapping("/records")
    public ApiResponse<List<SecondaryCommissionRecord>> records(
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.records(principal));
    }

    @PostMapping("/records/calculate")
    public ApiResponse<SecondaryCommissionRecord> calculate(
            @RequestBody SecondarySettlementService.CalculateRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.calculate(request, principal));
    }

    @PostMapping("/records/{id}/adjust")
    public ApiResponse<SecondaryCommissionRecord> adjust(
            @PathVariable Long id,
            @RequestBody AdjustRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.adjust(id, request.amount(), request.reason(), principal));
    }

    @PostMapping("/records/{id}/confirm")
    public ApiResponse<SecondaryCommissionRecord> confirm(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.confirm(id, principal));
    }

    public record AdjustRequest(BigDecimal amount, String reason) {}
}
