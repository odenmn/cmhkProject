package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.resource.entity.ReferralChain;
import com.cmhk.business.module.resource.entity.ReferralNumber;
import com.cmhk.business.module.resource.entity.ReferralNumberHistory;
import com.cmhk.business.module.resource.service.ReferralNumberService;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 管理端多接龙推荐号码接口。 */
@RestController
@RequestMapping("/api/admin/referral-numbers")
public class AdminReferralNumberController {
    private final ReferralNumberService service;

    public AdminReferralNumberController(ReferralNumberService service) {
        this.service = service;
    }

    @GetMapping("/chains")
    public ApiResponse<List<Map<String, Object>>> chains() {
        return ApiResponse.success(service.chains());
    }

    @PostMapping("/chains")
    public ApiResponse<ReferralChain> createChain(
            @RequestBody ChainRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.createChain(
                request.chainCode(),
                request.chainName(),
                request.remark(),
                principal));
    }

    @PostMapping("/chains/{id}/status")
    public ApiResponse<ReferralChain> changeStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.changeChainStatus(id, request.status(), request.reason(), principal));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> numbers(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.numbers(chainId, status, keyword));
    }

    @GetMapping("/eligible-orders")
    public ApiResponse<List<MobilePlanOrder>> eligibleOrders() {
        return ApiResponse.success(service.eligibleOrders());
    }

    @PostMapping("/chains/{id}/candidates")
    public ApiResponse<ReferralNumber> addCandidate(
            @PathVariable Long id,
            @RequestBody CandidateRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.addCandidate(id, request.referralNumber(), request.sourceReference(), principal));
    }

    @PostMapping("/chains/{id}/import/preview")
    public ApiResponse<Map<String, Object>> previewImport(
            @PathVariable Long id,
            @RequestParam MultipartFile file) {
        return ApiResponse.success(service.previewImport(id, file));
    }

    @PostMapping("/chains/{id}/import/confirm")
    public ApiResponse<Map<String, Object>> confirmImport(
            @PathVariable Long id,
            @RequestParam MultipartFile file,
            @RequestParam String fileHash,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.confirmImport(id, file, fileHash, principal));
    }

    @PostMapping("/chains/{id}/head")
    public ApiResponse<ReferralNumber> designateHead(
            @PathVariable Long id,
            @RequestBody HeadRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.designateHead(id, request.numberId(), request.reason(), principal));
    }

    @PostMapping("/chains/{id}/reserve")
    public ApiResponse<ReferralNumber> reserve(
            @PathVariable Long id,
            @RequestBody ReserveRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.reserve(id, request.orderId(), request.reason(), principal));
    }

    @PostMapping("/{id}/release")
    public ApiResponse<ReferralNumber> release(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.release(id, request.reason(), principal));
    }

    @PostMapping("/{id}/complete-onboarding")
    public ApiResponse<ReferralNumber> completeOnboarding(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.completeOnboarding(id, request.reason(), principal));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<ReferralNumber> disable(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.disable(id, request.reason(), principal));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<ReferralNumberHistory>> history(@PathVariable Long id) {
        return ApiResponse.success(service.history(id));
    }

    public record ChainRequest(String chainCode, String chainName, String remark) {}
    public record StatusRequest(String status, String reason) {}
    public record CandidateRequest(String referralNumber, String sourceReference) {}
    public record HeadRequest(Long numberId, String reason) {}
    public record ReserveRequest(Long orderId, String reason) {}
    public record ReasonRequest(String reason) {}
}
