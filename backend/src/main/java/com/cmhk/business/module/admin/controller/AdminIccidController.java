package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.entity.IccidAssignmentHistory;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.service.IccidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/** 管理端 ICCID 卡池接口。 */
@RestController
@RequestMapping("/api/admin/iccids")
public class AdminIccidController {

    private static final Logger log = LoggerFactory.getLogger(AdminIccidController.class);

    private final IccidService service;

    /** 单构造器由 Spring 自动注入，无需字段注入或额外的 @Autowired。 */
    public AdminIccidController(IccidService service) {
        this.service = service;
    }

    /** 查询卡池，并可按卡号、批次、状态、客户和订单筛选。 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String iccid,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String orderNo) {
        List<Map<String, Object>> rows = service.list(iccid, batch, status, phone, orderNo);
        log.info("管理端查询 ICCID 完成，数量={}", rows.size());
        return ApiResponse.success(rows);
    }

    /** 手工新增一张可用 ICCID。 */
    @PostMapping
    public ApiResponse<IccidInventory> create(
            @RequestBody CreateRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) {
        return ApiResponse.success(service.create(request.iccid(), request.batchNo(), request.remark(), operator));
    }

    /** 批量导入 ICCID 文件。 */
    @PostMapping("/import")
    public ApiResponse<Map<String, Integer>> importFile(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String batchNo,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) {
        return ApiResponse.success(service.importFile(file, batchNo, operator));
    }

    /** 将可用卡分配给指定客户及其订单。 */
    @PostMapping("/{id}/assign")
    public ApiResponse<IccidInventory> assign(
            @PathVariable Long id,
            @RequestBody AssignRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) {
        return ApiResponse.success(service.assign(
                id, request.customerId(), request.orderId(), request.reason(), operator));
    }

    /** 解除已占用 ICCID 的客户和订单绑定。 */
    @PostMapping("/{id}/unassign")
    public ApiResponse<IccidInventory> unassign(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) {
        return ApiResponse.success(service.unassign(id, request.reason(), operator));
    }

    /** 将已占用卡标记为实际使用。 */
    @PostMapping("/{id}/used")
    public ApiResponse<IccidInventory> used(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) {
        return ApiResponse.success(service.markUsed(id, operator));
    }

    /** 停用尚未分配的异常或失效 ICCID。 */
    @PostMapping("/{id}/disable")
    public ApiResponse<IccidInventory> disable(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) {
        return ApiResponse.success(service.disable(id, request.reason(), operator));
    }

    /** 查看单张 ICCID 的分配与状态变更历史。 */
    @GetMapping("/{id}/history")
    public ApiResponse<List<IccidAssignmentHistory>> history(@PathVariable Long id) {
        return ApiResponse.success(service.history(id));
    }

    public record CreateRequest(String iccid, String batchNo, String remark) {}

    public record AssignRequest(Long customerId, Long orderId, String reason) {}

    public record ReasonRequest(String reason) {}
}
