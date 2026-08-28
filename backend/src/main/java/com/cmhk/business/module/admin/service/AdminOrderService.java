package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.cashback.service.CashbackService;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.entity.OrderStatusCode;
import com.cmhk.business.module.mobile.entity.OrderStatusHistory;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.mobile.mapper.OrderStatusHistoryMapper;
import com.cmhk.business.module.task.service.OperationTaskService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import java.math.BigDecimal; import java.time.Duration; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter; import java.util.List; import java.util.concurrent.ThreadLocalRandom;

@Service
public class AdminOrderService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(1);
    private final MobilePlanOrderMapper mapper;
    private final CustomerMapper customerMapper;
    private final OperationLogService logService;
    private final CacheClient cacheClient;
    private final OrderStatusHistoryMapper historyMapper;
    private final MobilePlanMapper planMapper;
    private final OperationTaskService taskService;
    private final CashbackService cashbackService;
    private final JavaType orderListType;
    private final JavaType orderType;

    public AdminOrderService(
            MobilePlanOrderMapper mapper,
            CustomerMapper customerMapper,
            OperationLogService logService,
            CacheClient cacheClient,
            OrderStatusHistoryMapper historyMapper,
            MobilePlanMapper planMapper,
            OperationTaskService taskService,
            CashbackService cashbackService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.customerMapper = customerMapper;
        this.logService = logService;
        this.cacheClient = cacheClient;
        this.historyMapper = historyMapper;
        this.planMapper = planMapper;
        this.taskService = taskService;
        this.cashbackService = cashbackService;
        this.orderListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, MobilePlanOrder.class);
        this.orderType = objectMapper.getTypeFactory()
                .constructType(MobilePlanOrder.class);
    }

    public List<MobilePlanOrder> list(String keyword, String status, Long customerId) {
        String key=cacheClient.versionedKey(AdminCacheKeys.ORDERS,"list:"+AdminCacheKeys.discriminator(keyword,status,customerId));
        return cacheClient.queryWithMutex(key,orderListType,()->listFromDatabase(keyword,status,customerId),CACHE_TTL,EMPTY_CACHE_TTL);
    }

    public List<MobilePlanOrder> list(
            String keyword,
            String status,
            Long customerId,
            AdminPrincipal principal) {
        if (principal == null || !"CHANNEL".equals(principal.scopeType())) {
            return list(keyword, status, customerId);
        }
        List<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getChannelId, principal.scopeId()))
                .stream()
                .map(Customer::getId)
                .toList();
        if (customerIds.isEmpty() || (customerId != null && !customerIds.contains(customerId))) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(MobilePlanOrder::getOrderNo, keyword)
                        .or()
                        .like(MobilePlanOrder::getUmallOrderNo, keyword)
                        .or()
                        .like(MobilePlanOrder::getContactPhone, keyword)
                        .or()
                        .like(MobilePlanOrder::getServiceNumber, keyword))
                .eq(status != null && !status.isBlank(), MobilePlanOrder::getStatus, status)
                .eq(customerId != null, MobilePlanOrder::getCustomerId, customerId)
                .in(customerId == null, MobilePlanOrder::getCustomerId, customerIds)
                .orderByDesc(MobilePlanOrder::getId));
    }

    private List<MobilePlanOrder> listFromDatabase(String keyword, String status, Long customerId) {
        return mapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .and(keyword!=null&&!keyword.isBlank(), q->q.like(MobilePlanOrder::getOrderNo,keyword).or().like(MobilePlanOrder::getUmallOrderNo,keyword).or().like(MobilePlanOrder::getContactPhone,keyword).or().like(MobilePlanOrder::getServiceNumber,keyword))
                .eq(status!=null&&!status.isBlank(), MobilePlanOrder::getStatus,status)
                .eq(customerId!=null, MobilePlanOrder::getCustomerId,customerId).orderByDesc(MobilePlanOrder::getId));
    }

    @Transactional public MobilePlanOrder save(Long id, MobilePlanOrder input, String operator) {
        MobilePlanOrder before=id==null?null:required(id); MobilePlanOrder target=new MobilePlanOrder(); if(before!=null)BeanUtils.copyProperties(before,target);
        if (input.getCustomerId()==null || customerMapper.selectById(input.getCustomerId())==null) throw new IllegalArgumentException("请选择有效客户");
        MobilePlan plan = requiredPlan(input.getPlanId(), before);
        target.setCustomerId(input.getCustomerId()); target.setCustomerName(input.getCustomerName());
        if (input.getContactPhone() != null && !input.getContactPhone().isBlank()) {
            target.setContactPhone(input.getContactPhone().trim());
        }
        target.setPlanId(plan.getId()); target.setPlanCode(plan.getPlanCode()); target.setPlanName(plan.getPlanName());
        target.setPlanType(plan.getPlanType()); target.setMonthlyFee(plan.getMonthlyFee() == null ? BigDecimal.ZERO : plan.getMonthlyFee()); target.setContractPeriod(plan.getContractPeriod());
        target.setUmallOrderNo(input.getUmallOrderNo()); target.setServiceNumber(input.getServiceNumber()); target.setUmallStatus(input.getUmallStatus()); target.setReviewStatus(input.getReviewStatus()); target.setSupplementStatus(input.getSupplementStatus()); target.setActivationStatus(input.getActivationStatus()); target.setContractStatus(input.getContractStatus());
        target.setOrderSource(input.getOrderSource()==null?"ADMIN":input.getOrderSource()); target.setStatus(input.getStatus()==null?"待处理":input.getStatus());
        target.setReconciliationStatus(input.getReconciliationStatus()==null?"待对账":input.getReconciliationStatus());
        if(id==null){ target.setOrderNo("ADM"+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+ThreadLocalRandom.current().nextInt(100,1000)); mapper.insert(target); }
        else mapper.updateById(target);
        logService.record(operator,id==null?"ORDER_CREATE":"ORDER_UPDATE","ORDER",target.getId(),before,target,null);
        cacheClient.invalidateNamespacesAfterCommit(AdminCacheKeys.ORDERS,AdminCacheKeys.CUSTOMERS,AdminCacheKeys.ICCIDS,AdminCacheKeys.DASHBOARD);
        return target;
    }
    @Transactional
    public MobilePlanOrder save(Long id, MobilePlanOrder input, AdminPrincipal principal) {
        requireCustomerAccess(input.getCustomerId(), principal);
        MobilePlanOrder before = id == null ? null : required(id);
        if (before != null) {
            requireCustomerAccess(before.getCustomerId(), principal);
        }
        String targetStatus = input.getStatus() == null || input.getStatus().isBlank()
                ? OrderStatusCode.PENDING.name()
                : input.getStatus().trim();
        if (!OrderStatusCode.isSupported(targetStatus)) {
            throw new IllegalArgumentException("办理状态不在允许范围内");
        }
        input.setStatus(targetStatus);
        MobilePlanOrder saved = save(id, input, principal.username());
        applyActivatedAt(before, saved, input);
        if (before == null || !targetStatus.equals(before.getStatus())) {
            saved.setStatusUpdatedAt(LocalDateTime.now());
            mapper.updateById(saved);
            recordStatusHistory(before, saved, principal);
        }
        recordFieldHistory(saved.getId(), "UMALL", before == null ? null : before.getUmallStatus(), saved.getUmallStatus(), principal);
        recordFieldHistory(saved.getId(), "UMALL_REVIEW", before == null ? null : before.getReviewStatus(), saved.getReviewStatus(), principal);
        recordFieldHistory(saved.getId(), "UMALL_SUPPLEMENT", before == null ? null : before.getSupplementStatus(), saved.getSupplementStatus(), principal);
        recordFieldHistory(saved.getId(), "ACTIVATION", before == null ? null : before.getActivationStatus(), saved.getActivationStatus(), principal);
        recordFieldHistory(saved.getId(), "CONTRACT", before == null ? null : before.getContractStatus(), saved.getContractStatus(), principal);
        taskService.ensureTasksForOrderStatus(before, saved, principal);
        cashbackService.ensurePlanForOrder(saved, principal);
        return saved;
    }

    /** 保存明确录入的激活时间，并在首次标记为已激活时记录当前事实时间。 */
    private void applyActivatedAt(
            MobilePlanOrder before,
            MobilePlanOrder saved,
            MobilePlanOrder input) {
        if (input.getActivatedAt() != null) {
            if (!isActivated(saved)) {
                throw new IllegalArgumentException("实际激活时间只能用于已激活或已完成订单");
            }
            saved.setActivatedAt(input.getActivatedAt());
            mapper.updateById(saved);
            return;
        }
        if (!isActivated(before) && isActivated(saved) && saved.getActivatedAt() == null) {
            saved.setActivatedAt(LocalDateTime.now());
            mapper.updateById(saved);
        }
    }

    private boolean isActivated(MobilePlanOrder order) {
        return order != null
                && (OrderStatusCode.ACTIVATED.name().equals(order.getStatus())
                || OrderStatusCode.COMPLETED.name().equals(order.getStatus())
                || "已激活".equals(order.getActivationStatus()));
    }

    /** 记录统一办理状态变化，不回写历史记录。 */
    private void recordStatusHistory(
            MobilePlanOrder before,
            MobilePlanOrder saved,
            AdminPrincipal principal) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(saved.getId());
        history.setStatusType("JOINCOM");
        history.setBeforeStatus(before == null ? null : before.getStatus());
        history.setAfterStatus(saved.getStatus());
        history.setSourceType("ADMIN");
        history.setOperatorUserId(principal.userId());
        history.setOperatorName(principal.username());
        historyMapper.insert(history);
    }

    private void recordFieldHistory(
            Long orderId,
            String statusType,
            String beforeStatus,
            String afterStatus,
            AdminPrincipal principal) {
        if (afterStatus == null || afterStatus.equals(beforeStatus)) {
            return;
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setStatusType(statusType);
        history.setBeforeStatus(beforeStatus);
        history.setAfterStatus(afterStatus);
        history.setSourceType("ADMIN");
        history.setOperatorUserId(principal.userId());
        history.setOperatorName(principal.username());
        historyMapper.insert(history);
    }

    public MobilePlanOrder detail(Long id){String key=cacheClient.versionedKey(AdminCacheKeys.ORDERS,"detail:"+id);return cacheClient.queryWithMutex(key,orderType,()->required(id),CACHE_TTL,EMPTY_CACHE_TTL);}
    public MobilePlanOrder detail(Long id, AdminPrincipal principal) {
        MobilePlanOrder order = detail(id);
        requireCustomerAccess(order.getCustomerId(), principal);
        return order;
    }

    /** 查询订单完整状态历史，并复用订单的数据范围校验。 */
    public List<OrderStatusHistory> statusHistory(Long id, AdminPrincipal principal) {
        MobilePlanOrder order = required(id);
        requireCustomerAccess(order.getCustomerId(), principal);
        return historyMapper.selectList(new LambdaQueryWrapper<OrderStatusHistory>()
                .eq(OrderStatusHistory::getOrderId, id)
                .orderByDesc(OrderStatusHistory::getCreatedAt)
                .orderByDesc(OrderStatusHistory::getId));
    }

    private void requireCustomerAccess(Long customerId, AdminPrincipal principal) {
        Customer customer = customerId == null ? null : customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("请选择有效客户");
        }
        if (principal != null
                && "CHANNEL".equals(principal.scopeType())
                && !principal.scopeId().equals(customer.getChannelId())) {
            throw new IllegalArgumentException("当前账号不能访问其他渠道订单");
        }
    }
    public MobilePlanOrder required(Long id){MobilePlanOrder o=mapper.selectById(id);if(o==null)throw new IllegalArgumentException("订单不存在");return o;}
    /** 订单套餐只允许引用启用套餐；历史订单可保留原已停用套餐以便修正其他字段。 */
    private MobilePlan requiredPlan(Long planId, MobilePlanOrder before) {
        MobilePlan plan = planId == null ? null : planMapper.selectById(planId);
        boolean retainsHistoricalPlan = before != null && plan != null && plan.getId().equals(before.getPlanId());
        if (plan == null || (!Integer.valueOf(1).equals(plan.getEnabled()) && !retainsHistoricalPlan)) {
            throw new IllegalArgumentException("请选择已启用的套餐");
        }
        return plan;
    }
    private String required(String v,String m){if(v==null||v.isBlank())throw new IllegalArgumentException(m);return v.trim();}
}
