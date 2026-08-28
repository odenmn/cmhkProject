package com.cmhk.business.module.task.service;

import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.task.entity.OperationTask;

import java.util.List;
import java.util.Map;

/** 统一管理运营任务的创建、领取、处理和来源巡检。 */
public interface OperationTaskService {
    List<Map<String, Object>> list(TaskQuery query, AdminPrincipal principal);

    Map<String, Object> detail(Long taskId, AdminPrincipal principal);

    OperationTask create(TaskCreateRequest request, AdminPrincipal principal);

    OperationTask claim(Long taskId, String remark, AdminPrincipal principal);

    OperationTask reassign(Long taskId, Long assigneeUserId, String remark, AdminPrincipal principal);

    OperationTask process(Long taskId, String result, AdminPrincipal principal);

    OperationTask complete(Long taskId, String result, AdminPrincipal principal);

    OperationTask close(Long taskId, String reason, AdminPrincipal principal);

    void ensureTasksForReconciliation(ReconciliationRow row, MobilePlanOrder order);

    /**
     * 根据后台人工录入的订单状态补齐异常处理任务。
     *
     * @param order 已保存的订单
     * @param principal 当前后台操作人
     */
    void ensureTasksForOrderStatus(
            MobilePlanOrder before,
            MobilePlanOrder order,
            AdminPrincipal principal);

    Map<String, Integer> refreshResourceTasks(AdminPrincipal principal);

    record TaskQuery(
            String taskStatus,
            String taskType,
            Long assigneeUserId,
            Long customerId,
            Long orderId,
            String keyword) {
    }

    record TaskCreateRequest(
            String taskType,
            String title,
            String priority,
            Long customerId,
            Long orderId,
            Long channelId,
            String dueAt,
            String remark) {
    }
}
