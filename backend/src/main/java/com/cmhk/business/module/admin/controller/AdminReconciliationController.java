package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.entity.ReconciliationImport;
import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 管理端甲方对账导入、确认和异常匹配接口。 */
@RestController
@RequestMapping("/api/admin/reconciliation")
public class AdminReconciliationController {

    private static final Logger log = LoggerFactory.getLogger(AdminReconciliationController.class);

    private final ReconciliationService service;

    /** 构造器依赖由 Spring 自动注入。 */
    public AdminReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    /** 上传文件并生成预览，不修改正式订单。 */
    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(
            @RequestParam MultipartFile file,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        Map<String, Object> result = service.preview(file, principal.username());
        log.info("甲方对账文件预览完成");
        return ApiResponse.success(result);
    }

    /** 确认预览批次，写入可唯一匹配的正式订单状态。 */
    @PostMapping("/{id}/confirm")
    public ApiResponse<ReconciliationImport> confirm(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.confirm(id, principal.username()));
    }

    /** 查询历史导入批次。 */
    @GetMapping("/imports")
    public ApiResponse<List<ReconciliationImport>> imports() {
        return ApiResponse.success(service.batches());
    }

    /** 查询指定批次的导入明细。 */
    @GetMapping("/imports/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    /** 查询不能唯一自动匹配的对账行。 */
    @GetMapping("/exceptions")
    public ApiResponse<List<ReconciliationRow>> exceptions() {
        return ApiResponse.success(service.exceptions());
    }

    /** 将异常对账行人工关联到指定订单。 */
    @PostMapping("/rows/{id}/match")
    public ApiResponse<ReconciliationRow> match(
            @PathVariable Long id,
            @RequestBody MatchRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.manualMatch(id, request.orderId(), principal.username()));
    }

    public record MatchRequest(Long orderId) {}
}
