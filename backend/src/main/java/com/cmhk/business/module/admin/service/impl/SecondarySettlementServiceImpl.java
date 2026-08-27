package com.cmhk.business.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.entity.SecondaryChannel;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRule;
import com.cmhk.business.module.admin.mapper.SecondaryChannelMapper;
import com.cmhk.business.module.admin.mapper.SecondaryCommissionRecordMapper;
import com.cmhk.business.module.admin.mapper.SecondaryCommissionRuleMapper;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminCacheKeys;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.admin.service.SecondarySettlementService;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** 统一渠道佣金服务实现，旧二级渠道只保留只读兼容。 */
@Service
public class SecondarySettlementServiceImpl implements SecondarySettlementService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SecondaryChannelMapper legacyChannelMapper;
    private final ChannelMapper channelMapper;
    private final SecondaryCommissionRuleMapper ruleMapper;
    private final SecondaryCommissionRecordMapper recordMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final OperationLogService logService;
    private final ObjectMapper objectMapper;
    private final CacheClient cacheClient;

    public SecondarySettlementServiceImpl(
            SecondaryChannelMapper legacyChannelMapper,
            ChannelMapper channelMapper,
            SecondaryCommissionRuleMapper ruleMapper,
            SecondaryCommissionRecordMapper recordMapper,
            MobilePlanOrderMapper orderMapper,
            CustomerMapper customerMapper,
            OperationLogService logService,
            ObjectMapper objectMapper,
            CacheClient cacheClient) {
        this.legacyChannelMapper = legacyChannelMapper;
        this.channelMapper = channelMapper;
        this.ruleMapper = ruleMapper;
        this.recordMapper = recordMapper;
        this.orderMapper = orderMapper;
        this.customerMapper = customerMapper;
        this.logService = logService;
        this.objectMapper = objectMapper;
        this.cacheClient = cacheClient;
    }

    /** 旧二级渠道只读兼容查询，不再作为业务主档。 */
    @Override
    public List<SecondaryChannel> channels() {
        return legacyChannelMapper.selectList(
                new LambdaQueryWrapper<SecondaryChannel>().orderByDesc(SecondaryChannel::getId));
    }

    /** P1起停用旧渠道写入，调用方必须改用统一渠道接口。 */
    @Override
    public SecondaryChannel saveChannel(Long id, SecondaryChannel input, String operator) {
        throw new IllegalArgumentException("旧二级渠道写接口已停用，请使用统一渠道档案接口");
    }

    @Override
    public List<SecondaryCommissionRule> rules() {
        return ruleMapper.selectList(
                new LambdaQueryWrapper<SecondaryCommissionRule>().orderByDesc(SecondaryCommissionRule::getId));
    }

    @Transactional
    public SecondaryCommissionRule saveRule(Long id, SecondaryCommissionRule input, String operator) {
        SecondaryCommissionRule before = id == null ? null : requireRule(id);
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
                operator,
                id == null ? "COMMISSION_RULE_CREATE" : "COMMISSION_RULE_UPDATE",
                "COMMISSION_RULE",
                input.getId(),
                before,
                input,
                null);
        return input;
    }

    @Override
    @Transactional
    public SecondaryCommissionRule saveRule(
            Long id,
            SecondaryCommissionRule input,
            AdminPrincipal principal) {
        requireAdmin(principal);
        return saveRule(id, input, principal.username());
    }

    public List<SecondaryCommissionRecord> records() {
        return recordMapper.selectList(
                new LambdaQueryWrapper<SecondaryCommissionRecord>()
                        .orderByDesc(SecondaryCommissionRecord::getId));
    }

    @Override
    public List<SecondaryCommissionRecord> records(AdminPrincipal principal) {
        return recordMapper.selectList(
                new LambdaQueryWrapper<SecondaryCommissionRecord>()
                        .eq(
                                principal != null && "CHANNEL".equals(principal.scopeType()),
                                SecondaryCommissionRecord::getChannelId,
                                principal == null ? null : principal.scopeId())
                        .orderByDesc(SecondaryCommissionRecord::getId));
    }

    @Transactional
    public SecondaryCommissionRecord calculate(CalculateRequest request, String operator) {
        MobilePlanOrder order = orderMapper.selectById(request.orderId());
        Channel channel = requireChannel(request.channelId());
        SecondaryCommissionRule rule = requireRule(request.ruleId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (recordMapper.selectCount(new LambdaQueryWrapper<SecondaryCommissionRecord>()
                .eq(SecondaryCommissionRecord::getOrderId, request.orderId())) > 0) {
            throw new IllegalArgumentException("该订单已经生成结算记录");
        }
        if (!isActivated(order)) {
            throw new IllegalArgumentException("订单尚未激活，不能生成渠道结算");
        }
        if (!"已对账".equals(order.getReconciliationStatus())) {
            throw new IllegalArgumentException("订单尚未完成甲方对账");
        }

        BigDecimal fee = money(rule.getMonthlyFee());
        BigDecimal main = fee.multiply(number(rule.getMainMultiplier()));
        BigDecimal extra = fee.multiply(number(rule.getExtraMultiplier()));
        BigDecimal promotion = request.promotionApplied()
                ? fee.multiply(number(rule.getPromotionMultiplier()))
                : BigDecimal.ZERO;
        BigDecimal total = main.add(extra).add(promotion);
        BigDecimal gross = fee.multiply(number(rule.getChannelMultiplier()));
        BigDecimal channelSubsidy = request.channelSubsidy() == null
                ? money(rule.getDefaultChannelSubsidy())
                : money(request.channelSubsidy());
        BigDecimal joincomSubsidy = request.joincomSubsidy() == null
                ? money(rule.getDefaultJoincomSubsidy())
                : money(request.joincomSubsidy());
        BigDecimal payable = gross.subtract(channelSubsidy);
        BigDecimal retained = total.subtract(gross).subtract(joincomSubsidy);

        BigDecimal t1 = main.min(fee);
        BigDecimal cap = fee.multiply(BigDecimal.valueOf(
                rule.getContractMonths() != null && rule.getContractMonths() == 12 ? 2.5 : 3));
        BigDecimal t3 = main.subtract(t1)
                .max(BigDecimal.ZERO)
                .min(cap)
                .add(promotion);
        BigDecimal t7 = main.subtract(t1)
                .subtract(t3.subtract(promotion))
                .max(BigDecimal.ZERO)
                .add(extra);

        SecondaryCommissionRecord record = new SecondaryCommissionRecord();
        record.setOrderId(order.getId());
        record.setChannelId(channel.getId());
        record.setRuleId(rule.getId());
        record.setPromotionApplied(request.promotionApplied() ? 1 : 0);
        record.setJoincomTotal(money(total));
        record.setChannelGross(money(gross));
        record.setChannelSubsidy(channelSubsidy);
        record.setJoincomSubsidy(joincomSubsidy);
        record.setChannelPayable(money(payable));
        record.setJoincomRetained(money(retained));
        record.setT1Amount(money(t1));
        record.setT3Amount(money(t3));
        record.setT7Amount(money(t7));
        record.setAdjustmentAmount(ZERO);
        record.setFinalAmount(money(payable));
        record.setStatus("PENDING");
        try {
            record.setRuleSnapshot(objectMapper.writeValueAsString(rule));
        } catch (Exception exception) {
            throw new IllegalStateException("规则快照保存失败", exception);
        }
        recordMapper.insert(record);
        logService.record(
                operator,
                "COMMISSION_CALCULATE",
                "COMMISSION_RECORD",
                record.getId(),
                null,
                record,
                null);
        invalidateSettlementCaches();
        return record;
    }

    /** 渠道结算必须与订单客户的统一渠道归属一致。 */
    @Override
    @Transactional
    public SecondaryCommissionRecord calculate(CalculateRequest request, AdminPrincipal principal) {
        requireChannelAccess(request.channelId(), principal);
        MobilePlanOrder order = orderMapper.selectById(request.orderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null || !request.channelId().equals(customer.getChannelId())) {
            throw new IllegalArgumentException("结算渠道必须与订单客户归属渠道一致");
        }
        return calculate(request, principal.username());
    }

    @Transactional
    public SecondaryCommissionRecord adjust(
            Long id,
            BigDecimal amount,
            String reason,
            String operator) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("人工修正必须填写原因");
        }
        SecondaryCommissionRecord record = requireRecord(id);
        if ("CONFIRMED".equals(record.getStatus())) {
            throw new IllegalArgumentException("已确认结算不能修正");
        }
        BigDecimal before = record.getFinalAmount();
        record.setAdjustmentAmount(money(amount));
        record.setAdjustmentReason(reason);
        record.setFinalAmount(money(record.getChannelPayable().add(record.getAdjustmentAmount())));
        recordMapper.updateById(record);
        logService.record(
                operator,
                "COMMISSION_ADJUST",
                "COMMISSION_RECORD",
                id,
                before,
                record.getFinalAmount(),
                reason);
        invalidateSettlementCaches();
        return record;
    }

    @Override
    @Transactional
    public SecondaryCommissionRecord adjust(
            Long id,
            BigDecimal amount,
            String reason,
            AdminPrincipal principal) {
        requireAdmin(principal);
        return adjust(id, amount, reason, principal.username());
    }

    @Transactional
    public SecondaryCommissionRecord confirm(Long id, String operator) {
        SecondaryCommissionRecord record = requireRecord(id);
        if ("CONFIRMED".equals(record.getStatus())) {
            throw new IllegalArgumentException("结算记录已经确认");
        }
        String beforeStatus = record.getStatus();
        record.setStatus("CONFIRMED");
        record.setConfirmedBy(operator);
        record.setConfirmedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        logService.record(
                operator,
                "COMMISSION_CONFIRM",
                "COMMISSION_RECORD",
                id,
                beforeStatus,
                record.getStatus(),
                "仅确认业务结算，不触发自动打款");
        invalidateSettlementCaches();
        return record;
    }

    @Override
    @Transactional
    public SecondaryCommissionRecord confirm(Long id, AdminPrincipal principal) {
        requireAdmin(principal);
        return confirm(id, principal.username());
    }

    private void invalidateSettlementCaches() {
        cacheClient.invalidateNamespacesAfterCommit(AdminCacheKeys.DASHBOARD, AdminCacheKeys.CUSTOMERS);
    }

    private Channel requireChannel(Long id) {
        Channel value = channelMapper.selectById(id);
        if (value == null) {
            throw new IllegalArgumentException("统一渠道不存在");
        }
        return value;
    }

    private SecondaryCommissionRule requireRule(Long id) {
        SecondaryCommissionRule value = ruleMapper.selectById(id);
        if (value == null) {
            throw new IllegalArgumentException("佣金规则不存在");
        }
        return value;
    }

    private SecondaryCommissionRecord requireRecord(Long id) {
        SecondaryCommissionRecord value = recordMapper.selectById(id);
        if (value == null) {
            throw new IllegalArgumentException("结算记录不存在");
        }
        return value;
    }

    private void validateRule(SecondaryCommissionRule rule) {
        if (rule.getRuleName() == null || rule.getRuleName().isBlank()) {
            throw new IllegalArgumentException("规则名称不能为空");
        }
        if (rule.getMonthlyFee() == null || rule.getMonthlyFee().signum() < 0) {
            throw new IllegalArgumentException("月费必须大于等于0");
        }
    }

    private boolean isActivated(MobilePlanOrder order) {
        return (order.getActivationStatus() != null && order.getActivationStatus().contains("激活"))
                || "已激活".equals(order.getStatus());
    }

    private BigDecimal number(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return number(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void requireAdmin(AdminPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw new IllegalArgumentException("仅管理员可以执行此结算操作");
        }
    }

    private void requireChannelAccess(Long channelId, AdminPrincipal principal) {
        if (principal != null
                && "CHANNEL".equals(principal.scopeType())
                && !principal.scopeId().equals(channelId)) {
            throw new IllegalArgumentException("当前账号不能访问其他渠道结算");
        }
    }
}
