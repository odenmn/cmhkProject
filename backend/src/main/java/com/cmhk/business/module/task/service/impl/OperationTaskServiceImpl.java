package com.cmhk.business.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.mapper.AdminUserMapper;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.entity.OrderStatusCode;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.resource.entity.ReferralChain;
import com.cmhk.business.module.resource.mapper.ReferralChainMapper;
import com.cmhk.business.module.task.entity.OperationTask;
import com.cmhk.business.module.task.entity.OperationTaskHistory;
import com.cmhk.business.module.task.mapper.OperationTaskHistoryMapper;
import com.cmhk.business.module.task.mapper.OperationTaskMapper;
import com.cmhk.business.module.task.service.OperationTaskService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 统一任务中心实现，所有状态变更只记录内部处理事实。 */
@Service
public class OperationTaskServiceImpl implements OperationTaskService {
    private static final int ICCID_SHORTAGE_THRESHOLD = 10;
    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE = "DONE";
    private static final String CLOSED = "CLOSED";
    private static final Set<String> TASK_TYPES = Set.of(
            "CUSTOMER_FOLLOW_UP",
            "SUPPLEMENT",
            "REVIEW_EXCEPTION",
            "ACTIVATION_EXCEPTION",
            "RESOURCE_SHORTAGE",
            "RECONCILIATION_MATCH_EXCEPTION",
            "CASHBACK_EXCEPTION",
            "AFTER_SALES");

    private final OperationTaskMapper taskMapper;
    private final OperationTaskHistoryMapper historyMapper;
    private final CustomerMapper customerMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final ChannelMapper channelMapper;
    private final AdminUserMapper userMapper;
    private final IccidInventoryMapper iccidMapper;
    private final ReferralChainMapper chainMapper;
    private final OperationLogService logService;
    private final CacheClient cacheClient;

    public OperationTaskServiceImpl(
            OperationTaskMapper taskMapper,
            OperationTaskHistoryMapper historyMapper,
            CustomerMapper customerMapper,
            MobilePlanOrderMapper orderMapper,
            ChannelMapper channelMapper,
            AdminUserMapper userMapper,
            IccidInventoryMapper iccidMapper,
            ReferralChainMapper chainMapper,
            OperationLogService logService,
            CacheClient cacheClient) {
        this.taskMapper = taskMapper;
        this.historyMapper = historyMapper;
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.channelMapper = channelMapper;
        this.userMapper = userMapper;
        this.iccidMapper = iccidMapper;
        this.chainMapper = chainMapper;
        this.logService = logService;
        this.cacheClient = cacheClient;
    }

    /** 按服务端条件查询当前账号可处理的任务。 */
    @Override
    public List<Map<String, Object>> list(TaskQuery query, AdminPrincipal principal) {
        requireInternal(principal);
        LambdaQueryWrapper<OperationTask> wrapper = new LambdaQueryWrapper<OperationTask>()
                .eq(hasText(query.taskStatus()), OperationTask::getTaskStatus, query.taskStatus())
                .eq(hasText(query.taskType()), OperationTask::getTaskType, query.taskType())
                .eq(query.assigneeUserId() != null, OperationTask::getAssigneeUserId, query.assigneeUserId())
                .eq(query.customerId() != null, OperationTask::getCustomerId, query.customerId())
                .eq(query.orderId() != null, OperationTask::getOrderId, query.orderId())
                .and(hasText(query.keyword()), item -> item.like(OperationTask::getTaskNo, query.keyword())
                        .or()
                        .like(OperationTask::getTitle, query.keyword()))
                .orderByAsc(OperationTask::getTaskStatus)
                .orderByDesc(OperationTask::getCreatedAt);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toSummary)
                .toList();
    }

    /** 返回任务、关联业务对象与不可变处理历史。 */
    @Override
    public Map<String, Object> detail(Long taskId, AdminPrincipal principal) {
        requireInternal(principal);
        OperationTask task = required(taskId);
        Map<String, Object> result = new LinkedHashMap<>(toSummary(task));
        result.put("histories", historyMapper.selectList(new LambdaQueryWrapper<OperationTaskHistory>()
                .eq(OperationTaskHistory::getTaskId, taskId)
                .orderByDesc(OperationTaskHistory::getCreatedAt)
                .orderByDesc(OperationTaskHistory::getId)));
        return result;
    }

    /** 创建人工跟进或售后任务，不允许伪造外部状态。 */
    @Override
    @Transactional
    public OperationTask create(TaskCreateRequest request, AdminPrincipal principal) {
        requireInternal(principal);
        String taskType = requiredTaskType(request.taskType());
        return createTask(
                taskType,
                required(request.title(), "任务标题不能为空"),
                normalizePriority(request.priority()),
                request.customerId(),
                request.orderId(),
                request.channelId(),
                "MANUAL",
                UUID.randomUUID().toString(),
                parseDueAt(request.dueAt()),
                request.remark(),
                principal.userId(),
                principal.username());
    }

    /** 当前运营人员领取待处理任务。 */
    @Override
    @Transactional
    public OperationTask claim(Long taskId, String remark, AdminPrincipal principal) {
        requireInternal(principal);
        OperationTask task = required(taskId);
        if (!PENDING.equals(task.getTaskStatus())) {
            throw new IllegalArgumentException("只有待处理任务可以领取");
        }
        OperationTask before = copy(task);
        task.setTaskStatus(PROCESSING);
        task.setAssigneeUserId(principal.userId());
        task.setAssigneeName(principal.username());
        task.setClaimedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        recordHistory(task, "CLAIM", before, principal, remark);
        recordLog("TASK_CLAIM", task, before, principal, remark);
        invalidateCaches();
        return task;
    }

    /** 管理员将任务转派给指定内部账号。 */
    @Override
    @Transactional
    public OperationTask reassign(Long taskId, Long assigneeUserId, String remark, AdminPrincipal principal) {
        requireAdmin(principal);
        OperationTask task = requiredOpen(taskId);
        AdminUser assignee = requiredOperator(assigneeUserId);
        OperationTask before = copy(task);
        task.setTaskStatus(PROCESSING);
        task.setAssigneeUserId(assignee.getId());
        task.setAssigneeName(assignee.getUsername());
        task.setClaimedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        recordHistory(task, "REASSIGN", before, principal, required(remark, "转派原因不能为空"));
        recordLog("TASK_REASSIGN", task, before, principal, remark);
        invalidateCaches();
        return task;
    }

    /** 记录处理中间结论，不关闭任务。 */
    @Override
    @Transactional
    public OperationTask process(Long taskId, String result, AdminPrincipal principal) {
        requireHandler(taskId, principal);
        OperationTask task = requiredOpen(taskId);
        OperationTask before = copy(task);
        task.setTaskStatus(PROCESSING);
        task.setHandlingResult(required(result, "处理记录不能为空"));
        if (task.getAssigneeUserId() == null) {
            task.setAssigneeUserId(principal.userId());
            task.setAssigneeName(principal.username());
            task.setClaimedAt(LocalDateTime.now());
        }
        taskMapper.updateById(task);
        recordHistory(task, "PROCESS", before, principal, result);
        recordLog("TASK_PROCESS", task, before, principal, result);
        invalidateCaches();
        return task;
    }

    /** 标记内部处理动作完成，不修改订单或 UMALL 事实状态。 */
    @Override
    @Transactional
    public OperationTask complete(Long taskId, String result, AdminPrincipal principal) {
        requireHandler(taskId, principal);
        OperationTask task = requiredOpen(taskId);
        OperationTask before = copy(task);
        task.setTaskStatus(DONE);
        task.setHandlingResult(required(result, "完成说明不能为空"));
        task.setCompletedAt(LocalDateTime.now());
        task.setOpenDedupKey(null);
        taskMapper.updateById(task);
        recordHistory(task, "COMPLETE", before, principal, result);
        recordLog("TASK_COMPLETE", task, before, principal, result);
        invalidateCaches();
        return task;
    }

    /** 管理员关闭不再需要继续处理的任务。 */
    @Override
    @Transactional
    public OperationTask close(Long taskId, String reason, AdminPrincipal principal) {
        requireAdmin(principal);
        OperationTask task = requiredOpen(taskId);
        OperationTask before = copy(task);
        task.setTaskStatus(CLOSED);
        task.setHandlingResult(required(reason, "关闭原因不能为空"));
        task.setClosedAt(LocalDateTime.now());
        task.setOpenDedupKey(null);
        taskMapper.updateById(task);
        recordHistory(task, "CLOSE", before, principal, reason);
        recordLog("TASK_CLOSE", task, before, principal, reason);
        invalidateCaches();
        return task;
    }

    /** 根据已确认的 CMHK 对账行创建需要人工处理的任务。 */
    @Override
    @Transactional
    public void ensureTasksForReconciliation(ReconciliationRow row, MobilePlanOrder order) {
        if ("UNMATCHED".equals(row.getMatchStatus()) || "AMBIGUOUS".equals(row.getMatchStatus())) {
            ensureSourceTask("RECONCILIATION_MATCH_EXCEPTION", "CMHK 对账匹配异常", row, order);
        }
        if (order == null) {
            return;
        }
        if (OrderStatusCode.NEED_SUPPLEMENT.name().equals(order.getStatus())) {
            ensureSourceTask("SUPPLEMENT", "CMHK 补件处理", row, order);
        }
        if (containsFailure(row.getReviewStatus())) {
            ensureSourceTask("REVIEW_EXCEPTION", "CMHK 审核异常", row, order);
        }
        if (containsFailure(row.getActivationStatus())) {
            ensureSourceTask("ACTIVATION_EXCEPTION", "CMHK 激活异常", row, order);
        }
    }

    /** 根据后台编辑后的订单状态创建去重的人工处理任务。 */
    @Override
    @Transactional
    public void ensureTasksForOrderStatus(
            MobilePlanOrder before,
            MobilePlanOrder order,
            AdminPrincipal principal) {
        if (order == null || principal == null) {
            return;
        }
        if (needsSupplement(order) && !needsSupplement(before)) {
            ensureOrderTask("SUPPLEMENT", "订单补件处理", order, principal);
        }
        if (containsFailure(order.getReviewStatus())
                && !containsFailure(before == null ? null : before.getReviewStatus())) {
            ensureOrderTask("REVIEW_EXCEPTION", "订单审核异常", order, principal);
        }
        if (containsFailure(order.getActivationStatus())
                && !containsFailure(before == null ? null : before.getActivationStatus())) {
            ensureOrderTask("ACTIVATION_EXCEPTION", "订单激活异常", order, principal);
        }
    }

    /** 按当前库存和接龙状态补齐资源类任务。 */
    @Override
    @Transactional
    public Map<String, Integer> refreshResourceTasks(AdminPrincipal principal) {
        requireInternal(principal);
        int created = 0;
        long availableReal = iccidMapper.selectCount(new LambdaQueryWrapper<IccidInventory>()
                .eq(IccidInventory::getStatus, "AVAILABLE")
                .eq(IccidInventory::getCardType, "REAL"));
        if (availableReal < ICCID_SHORTAGE_THRESHOLD && ensureTask(
                "RESOURCE_SHORTAGE",
                "真实 ICCID 库存不足",
                "HIGH",
                null,
                null,
                null,
                "ICCID_STOCK",
                "REAL_AVAILABLE",
                null,
                "可用真实 ICCID 少于 " + ICCID_SHORTAGE_THRESHOLD + " 张",
                principal.userId(),
                principal.username()) != null) {
            created++;
        }
        for (ReferralChain chain : chainMapper.selectList(new LambdaQueryWrapper<ReferralChain>()
                .eq(ReferralChain::getStatus, "ACTIVE")
                .isNull(ReferralChain::getCurrentHeadNumberId))) {
            if (ensureTask(
                    "RESOURCE_SHORTAGE",
                    "推荐号码接龙中断",
                    "HIGH",
                    null,
                    null,
                    null,
                    "REFERRAL_CHAIN",
                    String.valueOf(chain.getId()),
                    null,
                    "活跃接龙缺少当前龙头：" + chain.getChainName(),
                    principal.userId(),
                    principal.username()) != null) {
                created++;
            }
        }
        return Map.of("created", created, "availableRealIccids", Math.toIntExact(availableReal));
    }

    private void ensureSourceTask(
            String taskType,
            String title,
            ReconciliationRow row,
            MobilePlanOrder order) {
        ensureTask(
                taskType,
                title,
                "HIGH",
                order == null ? null : order.getCustomerId(),
                order == null ? null : order.getId(),
                channelId(order),
                "RECONCILIATION_ROW",
                String.valueOf(row.getId()),
                null,
                row.getExceptionReason(),
                null,
                "SYSTEM");
    }

    /** 订单人工编辑和自动同步共用同一组受控状态，按订单和任务类型去重。 */
    private void ensureOrderTask(
            String taskType,
            String title,
            MobilePlanOrder order,
            AdminPrincipal principal) {
        ensureTask(
                taskType,
                title,
                "HIGH",
                order.getCustomerId(),
                order.getId(),
                channelId(order),
                "ORDER_STATUS",
                String.valueOf(order.getId()),
                null,
                "订单状态由后台人工更新触发",
                principal.userId(),
                principal.username());
    }

    private OperationTask ensureTask(
            String taskType,
            String title,
            String priority,
            Long customerId,
            Long orderId,
            Long channelId,
            String sourceType,
            String sourceRecordId,
            LocalDateTime dueAt,
            String remark,
            Long creatorId,
            String creatorName) {
        String dedupKey = sourceType + ":" + sourceRecordId + ":" + taskType;
        if (taskMapper.selectCount(new LambdaQueryWrapper<OperationTask>()
                .eq(OperationTask::getOpenDedupKey, dedupKey)) > 0) {
            return null;
        }
        try {
            return createTask(
                    taskType,
                    title,
                    priority,
                    customerId,
                    orderId,
                    channelId,
                    sourceType,
                    sourceRecordId,
                    dueAt,
                    remark,
                    creatorId,
                    creatorName);
        } catch (DuplicateKeyException exception) {
            return null;
        }
    }

    private OperationTask createTask(
            String taskType,
            String title,
            String priority,
            Long customerId,
            Long orderId,
            Long channelId,
            String sourceType,
            String sourceRecordId,
            LocalDateTime dueAt,
            String remark,
            Long creatorId,
            String creatorName) {
        String dedupKey = sourceType + ":" + sourceRecordId + ":" + taskType;
        OperationTask task = new OperationTask();
        task.setTaskNo("TASK-TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setTaskStatus(PENDING);
        task.setPriority(priority);
        task.setCustomerId(customerId);
        task.setOrderId(orderId);
        task.setChannelId(channelId);
        task.setSourceType(sourceType);
        task.setSourceRecordId(sourceRecordId);
        task.setDedupKey(dedupKey);
        task.setOpenDedupKey(dedupKey);
        task.setDueAt(dueAt);
        task.setHandlingResult(remark);
        task.setCreatedByUserId(creatorId);
        task.setCreatedByName(creatorName);
        taskMapper.insert(task);
        task.setTaskNo(String.format("TASK-%06d", task.getId()));
        taskMapper.updateById(task);
        recordHistory(task, "CREATE", null, creatorId, creatorName, remark);
        logService.record(creatorName, "TASK_CREATE", "OPERATION_TASK", task.getId(), null, task, remark);
        invalidateCaches();
        return task;
    }

    private Map<String, Object> toSummary(OperationTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("customer", task.getCustomerId() == null ? null : customerMapper.selectById(task.getCustomerId()));
        result.put("order", task.getOrderId() == null ? null : orderMapper.selectById(task.getOrderId()));
        result.put("channel", task.getChannelId() == null ? null : channelMapper.selectById(task.getChannelId()));
        return result;
    }

    private Long channelId(MobilePlanOrder order) {
        if (order == null || order.getCustomerId() == null) {
            return null;
        }
        Customer customer = customerMapper.selectById(order.getCustomerId());
        return customer == null ? null : customer.getChannelId();
    }

    private OperationTask required(Long id) {
        OperationTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return task;
    }

    private OperationTask requiredOpen(Long id) {
        OperationTask task = required(id);
        if (DONE.equals(task.getTaskStatus()) || CLOSED.equals(task.getTaskStatus())) {
            throw new IllegalArgumentException("已结束任务不能继续处理");
        }
        return task;
    }

    private AdminUser requiredOperator(Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null || !"ENABLED".equals(user.getStatus()) || !isInternalRole(user.getRoleCode())) {
            throw new IllegalArgumentException("只能转派给启用的内部管理员或运营人员");
        }
        return user;
    }

    private void requireHandler(Long taskId, AdminPrincipal principal) {
        requireInternal(principal);
        OperationTask task = requiredOpen(taskId);
        if (!principal.isAdmin() && !principal.userId().equals(task.getAssigneeUserId())) {
            throw new IllegalArgumentException("只能处理自己领取的任务");
        }
    }

    private void requireInternal(AdminPrincipal principal) {
        if (principal == null || !principal.isInternalOperator()) {
            throw new IllegalArgumentException("当前账号无任务中心权限");
        }
    }

    private void requireAdmin(AdminPrincipal principal) {
        requireInternal(principal);
        if (!principal.isAdmin()) {
            throw new IllegalArgumentException("只有管理员可以执行该任务操作");
        }
    }

    private void recordHistory(OperationTask task, String action, OperationTask before, AdminPrincipal principal, String remark) {
        recordHistory(task, action, before, principal == null ? null : principal.userId(), principal == null ? "SYSTEM" : principal.username(), remark);
    }

    private void recordHistory(
            OperationTask task,
            String action,
            OperationTask before,
            Long operatorId,
            String operatorName,
            String remark) {
        OperationTaskHistory history = new OperationTaskHistory();
        history.setTaskId(task.getId());
        history.setActionType(action);
        history.setBeforeStatus(before == null ? null : before.getTaskStatus());
        history.setAfterStatus(task.getTaskStatus());
        history.setBeforeAssigneeUserId(before == null ? null : before.getAssigneeUserId());
        history.setAfterAssigneeUserId(task.getAssigneeUserId());
        history.setOperatorUserId(operatorId);
        history.setOperatorName(operatorName);
        history.setRemark(remark);
        historyMapper.insert(history);
    }

    private void recordLog(String operation, OperationTask task, OperationTask before, AdminPrincipal principal, String remark) {
        logService.record(principal.username(), operation, "OPERATION_TASK", task.getId(), before, task, remark);
    }

    private void invalidateCaches() {
        cacheClient.invalidateNamespacesAfterCommit(
                AdminCacheKeys.TASKS,
                AdminCacheKeys.DASHBOARD,
                AdminCacheKeys.CUSTOMERS,
                AdminCacheKeys.ORDERS);
    }

    private String requiredTaskType(String value) {
        String taskType = required(value, "任务类型不能为空").toUpperCase();
        if (!TASK_TYPES.contains(taskType)) {
            throw new IllegalArgumentException("不支持的任务类型");
        }
        return taskType;
    }

    private String normalizePriority(String value) {
        String priority = hasText(value) ? value.toUpperCase() : "NORMAL";
        if (!Set.of("LOW", "NORMAL", "HIGH", "URGENT").contains(priority)) {
            throw new IllegalArgumentException("不支持的任务优先级");
        }
        return priority;
    }

    private LocalDateTime parseDueAt(String value) {
        return hasText(value) ? LocalDateTime.parse(value) : null;
    }

    private boolean containsFailure(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("拒绝")
                || normalized.contains("失败")
                || normalized.contains("异常")
                || normalized.contains("reject")
                || normalized.contains("fail")
                || normalized.contains("error");
    }

    /** 待补件状态既可来自标准订单状态，也可来自 CMHK 原始补件状态。 */
    private boolean containsSupplementNeeded(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("待补件")
                || normalized.contains("需补件")
                || normalized.contains("补件中")
                || normalized.contains("supplement");
    }

    private boolean needsSupplement(MobilePlanOrder order) {
        return order != null
                && (OrderStatusCode.NEED_SUPPLEMENT.name().equals(order.getStatus())
                || containsSupplementNeeded(order.getSupplementStatus()));
    }

    private boolean isInternalRole(String roleCode) {
        return "ADMIN".equals(roleCode) || "OPERATOR".equals(roleCode);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String required(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private OperationTask copy(OperationTask source) {
        OperationTask target = new OperationTask();
        target.setId(source.getId());
        target.setTaskNo(source.getTaskNo());
        target.setTaskType(source.getTaskType());
        target.setTitle(source.getTitle());
        target.setTaskStatus(source.getTaskStatus());
        target.setAssigneeUserId(source.getAssigneeUserId());
        target.setAssigneeName(source.getAssigneeName());
        target.setHandlingResult(source.getHandlingResult());
        return target;
    }
}
