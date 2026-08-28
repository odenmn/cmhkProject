package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.task.entity.OperationTask;
import com.cmhk.business.module.task.service.OperationTaskService;
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

import java.util.List;
import java.util.Map;

/** 管理端任务中心接口，只记录内部处理动作。 */
@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {
    private static final Logger log = LoggerFactory.getLogger(AdminTaskController.class);

    private final OperationTaskService service;

    public AdminTaskController(OperationTaskService service) {
        this.service = service;
    }

    /** 按状态、类型、关联客户或订单查询任务。 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) Long assigneeUserId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String keyword,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        List<Map<String, Object>> tasks = service.list(
                new OperationTaskService.TaskQuery(taskStatus, taskType, assigneeUserId, customerId, orderId, keyword),
                principal);
        log.info("查询运营任务完成，数量={}", tasks.size());
        return ApiResponse.success(tasks);
    }

    /** 查看任务关联关系及处理历史。 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.detail(id, principal));
    }

    /** 创建人工客户跟进、售后等任务。 */
    @PostMapping
    public ApiResponse<OperationTask> create(
            @RequestBody OperationTaskService.TaskCreateRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.create(request, principal));
    }

    /** 当前人员领取任务。 */
    @PostMapping("/{id}/claim")
    public ApiResponse<OperationTask> claim(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.claim(id, request.reason(), principal));
    }

    /** 管理员将任务转派给指定内部人员。 */
    @PostMapping("/{id}/reassign")
    public ApiResponse<OperationTask> reassign(
            @PathVariable Long id,
            @RequestBody ReassignRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.reassign(id, request.assigneeUserId(), request.reason(), principal));
    }

    /** 记录处理中间结论。 */
    @PostMapping("/{id}/process")
    public ApiResponse<OperationTask> process(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.process(id, request.reason(), principal));
    }

    /** 标记任务的内部处理动作已完成。 */
    @PostMapping("/{id}/complete")
    public ApiResponse<OperationTask> complete(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.complete(id, request.reason(), principal));
    }

    /** 管理员关闭无需继续处理的任务。 */
    @PostMapping("/{id}/close")
    public ApiResponse<OperationTask> close(
            @PathVariable Long id,
            @RequestBody ReasonRequest request,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.close(id, request.reason(), principal));
    }

    /** 人工触发资源巡检，补齐库存不足和接龙中断任务。 */
    @PostMapping("/refresh-resources")
    public ApiResponse<Map<String, Integer>> refreshResources(
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.refreshResourceTasks(principal));
    }

    public record ReasonRequest(String reason) {
    }

    public record ReassignRequest(Long assigneeUserId, String reason) {
    }
}
