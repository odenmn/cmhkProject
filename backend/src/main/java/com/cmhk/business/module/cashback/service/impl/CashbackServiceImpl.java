package com.cmhk.business.module.cashback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.cashback.entity.CustomerCashbackInstallment;
import com.cmhk.business.module.cashback.entity.CustomerCashbackPlan;
import com.cmhk.business.module.cashback.entity.CustomerCashbackRule;
import com.cmhk.business.module.cashback.mapper.CustomerCashbackInstallmentMapper;
import com.cmhk.business.module.cashback.mapper.CustomerCashbackPlanMapper;
import com.cmhk.business.module.cashback.mapper.CustomerCashbackRuleMapper;
import com.cmhk.business.module.cashback.service.CashbackService;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 客户返现服务实现，期次从实际激活日满一个月开始生成。 */
@Service
public class CashbackServiceImpl implements CashbackService {

    private final CustomerCashbackRuleMapper ruleMapper;
    private final CustomerCashbackPlanMapper planMapper;
    private final CustomerCashbackInstallmentMapper installmentMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final MobilePlanMapper mobilePlanMapper;
    private final CustomerMapper customerMapper;
    private final ChannelMapper channelMapper;
    private final OperationLogService logService;
    private final CacheClient cacheClient;
    private final ObjectMapper objectMapper;

    public CashbackServiceImpl(
            CustomerCashbackRuleMapper ruleMapper,
            CustomerCashbackPlanMapper planMapper,
            CustomerCashbackInstallmentMapper installmentMapper,
            MobilePlanOrderMapper orderMapper,
            MobilePlanMapper mobilePlanMapper,
            CustomerMapper customerMapper,
            ChannelMapper channelMapper,
            OperationLogService logService,
            CacheClient cacheClient,
            ObjectMapper objectMapper) {
        this.ruleMapper = ruleMapper;
        this.planMapper = planMapper;
        this.installmentMapper = installmentMapper;
        this.orderMapper = orderMapper;
        this.mobilePlanMapper = mobilePlanMapper;
        this.customerMapper = customerMapper;
        this.channelMapper = channelMapper;
        this.logService = logService;
        this.cacheClient = cacheClient;
        this.objectMapper = objectMapper;
    }

    /** 返回所有规则，只有管理员可以修改规则。 */
    @Override
    public List<CustomerCashbackRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<CustomerCashbackRule>()
                .orderByDesc(CustomerCashbackRule::getEnabled)
                .orderByDesc(CustomerCashbackRule::getId));
    }

    /** 保存规则时校验套餐存在、金额为正及有效期范围。 */
    @Override
    @Transactional
    public CustomerCashbackRule saveRule(
            Long id,
            CustomerCashbackRule input,
            AdminPrincipal principal) {
        requireAdmin(principal);
        CustomerCashbackRule before = id == null ? null : requiredRule(id);
        validateRule(input);
        input.setId(id);
        if (input.getEnabled() == null) {
            input.setEnabled(1);
        }
        if (id == null) {
            ruleMapper.insert(input);
        } else {
            ruleMapper.updateById(input);
        }
        logService.record(
                principal.username(),
                id == null ? "CASHBACK_RULE_CREATE" : "CASHBACK_RULE_UPDATE",
                "CASHBACK_RULE",
                input.getId(),
                before,
                input,
                null);
        invalidateCaches();
        return input;
    }

    /** 查询返现计划并按角色隐藏敏感金额。 */
    @Override
    public List<Map<String, Object>> plans(PlanQuery query, AdminPrincipal principal) {
        requireInternal(principal);
        return planMapper.selectList(new LambdaQueryWrapper<CustomerCashbackPlan>()
                        .eq(query.customerId() != null, CustomerCashbackPlan::getCustomerId, query.customerId())
                        .eq(query.orderId() != null, CustomerCashbackPlan::getOrderId, query.orderId())
                        .eq(query.channelId() != null, CustomerCashbackPlan::getChannelId, query.channelId())
                        .eq(hasText(query.status()), CustomerCashbackPlan::getStatus, query.status())
                        .orderByDesc(CustomerCashbackPlan::getId))
                .stream()
                .map(plan -> toSummary(plan, principal.isAdmin()))
                .toList();
    }

    /** 返回计划期次，非管理员不返回金额。 */
    @Override
    public List<CustomerCashbackInstallment> installments(
            Long cashbackPlanId,
            AdminPrincipal principal) {
        requireInternal(principal);
        requiredPlan(cashbackPlanId);
        return installmentMapper.selectList(new LambdaQueryWrapper<CustomerCashbackInstallment>()
                        .eq(CustomerCashbackInstallment::getCashbackPlanId, cashbackPlanId)
                        .orderByAsc(CustomerCashbackInstallment::getInstallmentNo))
                .stream()
                .map(item -> maskInstallment(item, principal.isAdmin()))
                .toList();
    }

    /** 管理员可按订单套餐提前生成返现计划，激活后再生成期次。 */
    @Override
    @Transactional
    public CustomerCashbackPlan generateForOrder(Long orderId, AdminPrincipal principal) {
        requireAdmin(principal);
        MobilePlanOrder order = requiredOrder(orderId);
        return createPlan(order, principal, true);
    }

    /** 批量为现有订单生成返现计划，无法唯一匹配的订单只统计不覆盖。 */
    @Override
    @Transactional
    public Map<String, Object> generateForExistingOrders(AdminPrincipal principal) {
        requireAdmin(principal);
        List<MobilePlanOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .orderByAsc(MobilePlanOrder::getId));
        int generated = 0;
        int existing = 0;
        int unmatched = 0;
        int pendingActivation = 0;
        int active = 0;
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (MobilePlanOrder order : orders) {
            CustomerCashbackPlan before = findPlanByOrderId(order.getId());
            try {
                CustomerCashbackPlan plan = createPlan(order, principal, false);
                if (plan == null) {
                    unmatched++;
                    continue;
                }
                if (before == null) {
                    generated++;
                } else {
                    existing++;
                }
                if ("PENDING_ACTIVATION".equals(plan.getStatus())) {
                    pendingActivation++;
                } else {
                    active++;
                }
            } catch (IllegalArgumentException exception) {
                conflicts.add(Map.of(
                        "orderId", order.getId(),
                        "reason", exception.getMessage()));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", orders.size());
        result.put("generated", generated);
        result.put("existing", existing);
        result.put("unmatched", unmatched);
        result.put("pendingActivation", pendingActivation);
        result.put("active", active);
        result.put("conflictCount", conflicts.size());
        result.put("conflicts", conflicts);
        logService.record(
                principal.username(),
                "CASHBACK_PLAN_BATCH_GENERATE",
                "CASHBACK_PLAN",
                null,
                null,
                result,
                "按现有订单套餐批量生成返现计划");
        invalidateCaches();
        return result;
    }

    /** 期次人工确认不代表自动付款，只记录内部确认动作。 */
    @Override
    @Transactional
    public CustomerCashbackInstallment confirmInstallment(
            Long installmentId,
            String remark,
            AdminPrincipal principal) {
        requireAdmin(principal);
        if (!hasText(remark)) {
            throw new IllegalArgumentException("确认返现期次必须填写说明");
        }
        CustomerCashbackInstallment installment = requiredInstallment(installmentId);
        if (!"PENDING".equals(installment.getStatus())) {
            throw new IllegalArgumentException("只有待确认期次可以确认");
        }
        CustomerCashbackInstallment before = copyInstallment(installment);
        installment.setStatus("CONFIRMED");
        installment.setConfirmedByUserId(principal.userId());
        installment.setConfirmedByName(principal.username());
        installment.setConfirmedAt(LocalDateTime.now());
        installment.setConfirmationRemark(remark.trim());
        installmentMapper.updateById(installment);
        CustomerCashbackPlan plan = requiredPlan(installment.getCashbackPlanId());
        boolean allConfirmed = installmentMapper.selectCount(new LambdaQueryWrapper<CustomerCashbackInstallment>()
                .eq(CustomerCashbackInstallment::getCashbackPlanId, plan.getId())
                .ne(CustomerCashbackInstallment::getStatus, "CONFIRMED")) == 0;
        if (allConfirmed) {
            plan.setStatus("COMPLETED");
            planMapper.updateById(plan);
        }
        logService.record(
                principal.username(),
                "CASHBACK_INSTALLMENT_CONFIRM",
                "CASHBACK_INSTALLMENT",
                installment.getId(),
                before,
                installment,
                installment.getConfirmationRemark());
        invalidateCaches();
        return installment;
    }

    /** 订单选定套餐后生成计划；补录实际激活时间后再生成返现期次。 */
    @Override
    @Transactional
    public void ensurePlanForOrder(MobilePlanOrder order, AdminPrincipal principal) {
        if (order == null) {
            return;
        }
        createPlan(order, principal, false);
    }

    private CustomerCashbackPlan createPlan(
            MobilePlanOrder order,
            AdminPrincipal principal,
            boolean ruleRequired) {
        CustomerCashbackPlan existing = findPlanByOrderId(order.getId());
        if (existing != null) {
            activateExistingPlan(existing, order, principal);
            return existing;
        }
        RuleMatch match = matchingRule(order, ruleRequired);
        if (match == null) {
            return null;
        }
        CustomerCashbackRule rule = match.rule();
        Customer customer = requiredCustomer(order.getCustomerId());
        MobilePlan mobilePlan = match.mobilePlan();
        CustomerCashbackPlan plan = new CustomerCashbackPlan();
        plan.setPlanNo("CB-TMP-" + Long.toUnsignedString(System.nanoTime()));
        plan.setCustomerId(customer.getId());
        plan.setOrderId(order.getId());
        plan.setChannelId(customer.getChannelId());
        plan.setCashbackRuleId(rule.getId());
        plan.setRuleSnapshot(ruleSnapshot(rule, mobilePlan));
        plan.setActivatedAt(order.getActivatedAt());
        plan.setInstallmentCount(rule.getContractMonths());
        plan.setTotalAmount(money(rule.getInstallmentAmount().multiply(BigDecimal.valueOf(rule.getContractMonths()))));
        plan.setStatus(order.getActivatedAt() == null ? "PENDING_ACTIVATION" : "ACTIVE");
        plan.setGeneratedByUserId(principal == null ? null : principal.userId());
        plan.setGeneratedByName(principal == null ? "SYSTEM" : principal.username());
        plan.setGeneratedAt(LocalDateTime.now());
        try {
            planMapper.insert(plan);
        } catch (DuplicateKeyException exception) {
            return planMapper.selectOne(new LambdaQueryWrapper<CustomerCashbackPlan>()
                    .eq(CustomerCashbackPlan::getOrderId, order.getId()));
        }
        plan.setPlanNo(String.format("CB-%06d", plan.getId()));
        planMapper.updateById(plan);
        if (order.getActivatedAt() != null) {
            createInstallments(plan, order.getActivatedAt(), rule.getInstallmentAmount());
        }
        logService.record(
                principal == null ? "SYSTEM" : principal.username(),
                "CASHBACK_PLAN_GENERATE",
                "CASHBACK_PLAN",
                plan.getId(),
                null,
                plan,
                order.getActivatedAt() == null
                        ? "订单已选定返现套餐，等待实际激活后生成期次"
                        : "返现计划按实际激活日满一个月生成首期");
        invalidateCaches();
        return plan;
    }

    private Map<String, Object> toSummary(CustomerCashbackPlan plan, boolean showAmounts) {
        Map<String, Object> result = new LinkedHashMap<>();
        CustomerCashbackPlan visiblePlan = copyPlan(plan);
        if (!showAmounts) {
            visiblePlan.setTotalAmount(null);
        }
        result.put("cashbackPlan", visiblePlan);
        result.put("customer", customerMapper.selectById(plan.getCustomerId()));
        result.put("order", orderMapper.selectById(plan.getOrderId()));
        result.put("channel", plan.getChannelId() == null ? null : channelMapper.selectById(plan.getChannelId()));
        return result;
    }

    private CustomerCashbackInstallment maskInstallment(
            CustomerCashbackInstallment source,
            boolean showAmounts) {
        CustomerCashbackInstallment target = copyInstallment(source);
        if (!showAmounts) {
            target.setPlannedAmount(null);
        }
        return target;
    }

    private RuleMatch matchingRule(MobilePlanOrder order, boolean required) {
        LocalDate ruleDate = order.getActivatedAt() == null
                ? LocalDate.now()
                : order.getActivatedAt().toLocalDate();
        Integer contractMonths = contractMonths(order.getContractPeriod());
        List<CustomerCashbackRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<CustomerCashbackRule>()
                .eq(order.getPlanId() != null, CustomerCashbackRule::getPlanId, order.getPlanId())
                .eq(contractMonths != null, CustomerCashbackRule::getContractMonths, contractMonths)
                .eq(CustomerCashbackRule::getEnabled, 1)
                .and(item -> item.isNull(CustomerCashbackRule::getEffectiveFrom)
                        .or()
                        .le(CustomerCashbackRule::getEffectiveFrom, ruleDate))
                .and(item -> item.isNull(CustomerCashbackRule::getEffectiveTo)
                        .or()
                        .ge(CustomerCashbackRule::getEffectiveTo, ruleDate)));
        List<RuleMatch> matches = rules.stream()
                .map(rule -> new RuleMatch(rule, mobilePlanMapper.selectById(rule.getPlanId())))
                .filter(match -> match.mobilePlan() != null)
                .filter(match -> order.getPlanId() != null
                        || matchesSnapshotPlan(order.getPlanName(), match.mobilePlan().getPlanName()))
                .toList();
        if (matches.isEmpty() && required) {
            throw new IllegalArgumentException("没有匹配当前套餐和合约期的启用返现规则");
        }
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("当前套餐和合约期匹配多条返现规则，请先停用冲突规则");
        }
        return matches.getFirst();
    }

    private boolean matchesSnapshotPlan(String orderPlanName, String configuredPlanName) {
        if (!hasText(orderPlanName) || !hasText(configuredPlanName)) {
            return false;
        }
        return normalizePlanName(orderPlanName).equals(normalizePlanName(configuredPlanName));
    }

    private String normalizePlanName(String value) {
        return value.toUpperCase()
                .replace("学生", "")
                .replace("5G", "")
                .replaceAll("[\\s_-]", "");
    }

    private CustomerCashbackPlan findPlanByOrderId(Long orderId) {
        return planMapper.selectOne(new LambdaQueryWrapper<CustomerCashbackPlan>()
                .eq(CustomerCashbackPlan::getOrderId, orderId));
    }

    /** 待激活计划在补录真实激活时间后一次性生成全部期次。 */
    private void activateExistingPlan(
            CustomerCashbackPlan plan,
            MobilePlanOrder order,
            AdminPrincipal principal) {
        if (order.getActivatedAt() == null || plan.getActivatedAt() != null) {
            return;
        }
        CustomerCashbackRule rule = requiredRule(plan.getCashbackRuleId());
        plan.setActivatedAt(order.getActivatedAt());
        plan.setStatus("ACTIVE");
        planMapper.updateById(plan);
        createInstallments(plan, order.getActivatedAt(), rule.getInstallmentAmount());
        logService.record(
                principal == null ? "SYSTEM" : principal.username(),
                "CASHBACK_PLAN_ACTIVATE",
                "CASHBACK_PLAN",
                plan.getId(),
                null,
                plan,
                "按实际激活日生成返现期次");
        invalidateCaches();
    }

    private void createInstallments(
            CustomerCashbackPlan plan,
            LocalDateTime activatedAt,
            BigDecimal installmentAmount) {
        if (installmentMapper.selectCount(new LambdaQueryWrapper<CustomerCashbackInstallment>()
                .eq(CustomerCashbackInstallment::getCashbackPlanId, plan.getId())) > 0) {
            return;
        }
        for (int installmentNo = 1; installmentNo <= plan.getInstallmentCount(); installmentNo++) {
            CustomerCashbackInstallment installment = new CustomerCashbackInstallment();
            installment.setCashbackPlanId(plan.getId());
            installment.setInstallmentNo(installmentNo);
            installment.setPlannedAmount(money(installmentAmount));
            installment.setPlannedDate(activatedAt.toLocalDate().plusMonths(installmentNo));
            installment.setStatus("PENDING");
            installmentMapper.insert(installment);
        }
    }

    private String ruleSnapshot(CustomerCashbackRule rule, MobilePlan mobilePlan) {
        try {
            return objectMapper.writeValueAsString(Map.of("rule", rule, "mobilePlan", mobilePlan));
        } catch (Exception exception) {
            throw new IllegalStateException("返现规则快照保存失败", exception);
        }
    }

    private void validateRule(CustomerCashbackRule rule) {
        if (!hasText(rule.getRuleName())) {
            throw new IllegalArgumentException("返现规则名称不能为空");
        }
        requiredMobilePlan(rule.getPlanId());
        if (rule.getContractMonths() == null || rule.getContractMonths() <= 0) {
            throw new IllegalArgumentException("返现合约期必须大于0");
        }
        if (rule.getInstallmentAmount() == null || rule.getInstallmentAmount().signum() <= 0) {
            throw new IllegalArgumentException("每期返现金额必须大于0");
        }
        if (rule.getEffectiveFrom() != null
                && rule.getEffectiveTo() != null
                && rule.getEffectiveFrom().isAfter(rule.getEffectiveTo())) {
            throw new IllegalArgumentException("返现规则生效日期不能晚于失效日期");
        }
    }

    private Integer contractMonths(String contractPeriod) {
        if (!hasText(contractPeriod)) {
            return null;
        }
        String digits = contractPeriod.replaceAll("\\D", "");
        return digits.isEmpty() ? null : Integer.valueOf(digits);
    }

    private CustomerCashbackRule requiredRule(Long id) {
        CustomerCashbackRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("返现规则不存在");
        }
        return rule;
    }

    private CustomerCashbackPlan requiredPlan(Long id) {
        CustomerCashbackPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("返现计划不存在");
        }
        return plan;
    }

    private CustomerCashbackInstallment requiredInstallment(Long id) {
        CustomerCashbackInstallment installment = installmentMapper.selectById(id);
        if (installment == null) {
            throw new IllegalArgumentException("返现期次不存在");
        }
        return installment;
    }

    private MobilePlanOrder requiredOrder(Long id) {
        MobilePlanOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private Customer requiredCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("订单客户不存在");
        }
        return customer;
    }

    private MobilePlan requiredMobilePlan(Long id) {
        MobilePlan plan = mobilePlanMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("套餐不存在");
        }
        return plan;
    }

    private CustomerCashbackPlan copyPlan(CustomerCashbackPlan source) {
        CustomerCashbackPlan target = new CustomerCashbackPlan();
        target.setId(source.getId());
        target.setPlanNo(source.getPlanNo());
        target.setCustomerId(source.getCustomerId());
        target.setOrderId(source.getOrderId());
        target.setChannelId(source.getChannelId());
        target.setCashbackRuleId(source.getCashbackRuleId());
        target.setActivatedAt(source.getActivatedAt());
        target.setTotalAmount(source.getTotalAmount());
        target.setInstallmentCount(source.getInstallmentCount());
        target.setStatus(source.getStatus());
        target.setGeneratedAt(source.getGeneratedAt());
        return target;
    }

    private CustomerCashbackInstallment copyInstallment(CustomerCashbackInstallment source) {
        CustomerCashbackInstallment target = new CustomerCashbackInstallment();
        target.setId(source.getId());
        target.setCashbackPlanId(source.getCashbackPlanId());
        target.setInstallmentNo(source.getInstallmentNo());
        target.setPlannedAmount(source.getPlannedAmount());
        target.setPlannedDate(source.getPlannedDate());
        target.setStatus(source.getStatus());
        target.setConfirmedByUserId(source.getConfirmedByUserId());
        target.setConfirmedByName(source.getConfirmedByName());
        target.setConfirmedAt(source.getConfirmedAt());
        target.setConfirmationRemark(source.getConfirmationRemark());
        return target;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void requireInternal(AdminPrincipal principal) {
        if (principal == null || !principal.isInternalOperator()) {
            throw new IllegalArgumentException("当前账号无返现计划权限");
        }
    }

    private void requireAdmin(AdminPrincipal principal) {
        requireInternal(principal);
        if (!principal.isAdmin()) {
            throw new IllegalArgumentException("只有管理员可以修改返现规则或确认返现期次");
        }
    }

    private void invalidateCaches() {
        cacheClient.invalidateNamespacesAfterCommit(
                AdminCacheKeys.CASHBACKS,
                AdminCacheKeys.DASHBOARD,
                AdminCacheKeys.CUSTOMERS,
                AdminCacheKeys.ORDERS);
    }

    private record RuleMatch(CustomerCashbackRule rule, MobilePlan mobilePlan) {
    }
}
