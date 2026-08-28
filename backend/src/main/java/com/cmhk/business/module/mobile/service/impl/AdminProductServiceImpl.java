package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.mobile.entity.ChannelProductPolicy;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOffer;
import com.cmhk.business.module.mobile.mapper.ChannelProductPolicyMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOfferMapper;
import com.cmhk.business.module.mobile.service.AdminProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 产品管理实现；套餐下架代替物理删除，确保历史订单快照可追溯。 */
@Service
public class AdminProductServiceImpl implements AdminProductService {

    private static final String MOBILE_PLAN_CACHE_KEY = "cmhk:mobile-plan:list:enabled";

    private final MobilePlanMapper planMapper;
    private final MobilePlanOfferMapper offerMapper;
    private final ChannelProductPolicyMapper policyMapper;
    private final ChannelMapper channelMapper;
    private final OperationLogService logService;
    private final CacheClient cacheClient;

    public AdminProductServiceImpl(
            MobilePlanMapper planMapper,
            MobilePlanOfferMapper offerMapper,
            ChannelProductPolicyMapper policyMapper,
            ChannelMapper channelMapper,
            OperationLogService logService,
            CacheClient cacheClient) {
        this.planMapper = planMapper;
        this.offerMapper = offerMapper;
        this.policyMapper = policyMapper;
        this.channelMapper = channelMapper;
        this.logService = logService;
        this.cacheClient = cacheClient;
    }

    @Override
    public List<MobilePlan> listPlans() {
        List<MobilePlan> plans = planMapper.selectList(new LambdaQueryWrapper<MobilePlan>()
                .orderByAsc(MobilePlan::getSortOrder)
                .orderByDesc(MobilePlan::getId));
        for (MobilePlan plan : plans) {
            plan.setOffers(listOffers(plan.getPlanCode()));
        }
        return plans;
    }

    @Override
    @Transactional
    public MobilePlan savePlan(Long id, MobilePlan input, String operator) {
        MobilePlan before = id == null ? null : requiredPlan(id);
        MobilePlan target = new MobilePlan();
        if (before != null) {
            BeanUtils.copyProperties(before, target);
        }
        target.setPlanCode(requiredText(input.getPlanCode(), "套餐编码不能为空"));
        target.setPlanName(requiredText(input.getPlanName(), "套餐名称不能为空"));
        target.setPlanType(requiredText(input.getPlanType(), "套餐类型不能为空"));
        target.setMonthlyFee(input.getMonthlyFee() == null ? BigDecimal.ZERO : input.getMonthlyFee());
        target.setChannelPriceText(requiredText(input.getChannelPriceText(), "渠道价格说明不能为空"));
        target.setEffectiveMonthlyFee(input.getEffectiveMonthlyFee());
        target.setEffectivePriceText(input.getEffectivePriceText());
        target.setOfficialMonthlyFee(input.getOfficialMonthlyFee());
        target.setOfficialPriceText(input.getOfficialPriceText());
        target.setDataQuota(requiredText(input.getDataQuota(), "流量权益不能为空"));
        target.setVoiceQuota(input.getVoiceQuota());
        target.setRoamingBenefit(input.getRoamingBenefit());
        target.setContractPeriod(input.getContractPeriod());
        target.setPromotionEndDate(input.getPromotionEndDate());
        target.setSourceVersion(input.getSourceVersion());
        target.setDiscountFormula(input.getDiscountFormula());
        target.setDescription(input.getDescription());
        target.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
        target.setEnabled(input.getEnabled() == null ? 1 : input.getEnabled());
        if (id == null) {
            planMapper.insert(target);
        } else {
            planMapper.updateById(target);
        }
        logService.record(operator, id == null ? "PLAN_CREATE" : "PLAN_UPDATE", "MOBILE_PLAN", target.getId(), before, target, null);
        cacheClient.delete(MOBILE_PLAN_CACHE_KEY);
        return target;
    }

    @Override
    @Transactional
    public void disablePlan(Long id, String operator) {
        MobilePlan target = requiredPlan(id);
        MobilePlan before = new MobilePlan();
        BeanUtils.copyProperties(target, before);
        target.setEnabled(0);
        planMapper.updateById(target);
        logService.record(operator, "PLAN_DISABLE", "MOBILE_PLAN", id, before, target, null);
        cacheClient.delete(MOBILE_PLAN_CACHE_KEY);
    }

    @Override
    public List<MobilePlanOffer> listOffers(String planCode) {
        return offerMapper.selectList(new LambdaQueryWrapper<MobilePlanOffer>()
                .eq(MobilePlanOffer::getPlanCode, planCode)
                .orderByAsc(MobilePlanOffer::getSortOrder)
                .orderByAsc(MobilePlanOffer::getId));
    }

    @Override
    @Transactional
    public MobilePlanOffer saveOffer(Long id, MobilePlanOffer input, String operator) {
        MobilePlanOffer before = id == null ? null : requiredOffer(id);
        MobilePlanOffer target = new MobilePlanOffer();
        if (before != null) {
            BeanUtils.copyProperties(before, target);
        }
        target.setPlanCode(requiredText(input.getPlanCode(), "权益所属套餐不能为空"));
        requirePlanCode(target.getPlanCode());
        target.setOfferType(requiredText(input.getOfferType(), "权益类型不能为空"));
        target.setOfferName(requiredText(input.getOfferName(), "权益名称不能为空"));
        target.setOfferValue(requiredText(input.getOfferValue(), "权益内容不能为空"));
        target.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
        target.setEnabled(input.getEnabled() == null ? 1 : input.getEnabled());
        if (id == null) {
            offerMapper.insert(target);
        } else {
            offerMapper.updateById(target);
        }
        logService.record(operator, id == null ? "OFFER_CREATE" : "OFFER_UPDATE", "MOBILE_PLAN_OFFER", target.getId(), before, target, null);
        cacheClient.delete(MOBILE_PLAN_CACHE_KEY);
        return target;
    }

    @Override
    @Transactional
    public void deleteOffer(Long id, String operator) {
        MobilePlanOffer before = requiredOffer(id);
        offerMapper.deleteById(id);
        logService.record(operator, "OFFER_DELETE", "MOBILE_PLAN_OFFER", id, before, null, null);
        cacheClient.delete(MOBILE_PLAN_CACHE_KEY);
    }

    @Override
    public List<Map<String, Object>> listPolicies() {
        List<ChannelProductPolicy> policies = policyMapper.selectList(
                new LambdaQueryWrapper<ChannelProductPolicy>().orderByDesc(ChannelProductPolicy::getId));
        return policies.stream().map(policy -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("policy", policy);
            Channel channel = channelMapper.selectById(policy.getChannelId());
            MobilePlan plan = planMapper.selectById(policy.getPlanId());
            row.put("channelName", channel == null ? null : channel.getChannelName());
            row.put("planName", plan == null ? null : plan.getPlanName());
            return row;
        }).toList();
    }

    @Override
    @Transactional
    public ChannelProductPolicy savePolicy(Long id, ChannelProductPolicy input, String operator) {
        ChannelProductPolicy before = id == null ? null : requiredPolicy(id);
        if (channelMapper.selectById(input.getChannelId()) == null) {
            throw new IllegalArgumentException("渠道不存在");
        }
        requiredPlan(input.getPlanId());
        if (input.getEffectiveFrom() != null
                && input.getEffectiveTo() != null
                && input.getEffectiveFrom().isAfter(input.getEffectiveTo())) {
            throw new IllegalArgumentException("政策生效日期不能晚于失效日期");
        }
        ChannelProductPolicy target = new ChannelProductPolicy();
        if (before != null) {
            BeanUtils.copyProperties(before, target);
        }
        target.setChannelId(input.getChannelId());
        target.setPlanId(input.getPlanId());
        target.setPromotable(input.getPromotable() == null ? 1 : input.getPromotable());
        target.setEffectiveFrom(input.getEffectiveFrom());
        target.setEffectiveTo(input.getEffectiveTo());
        target.setCashbackRuleRef(input.getCashbackRuleRef());
        target.setCommissionRuleRef(input.getCommissionRuleRef());
        if (id == null) {
            policyMapper.insert(target);
        } else {
            policyMapper.updateById(target);
        }
        logService.record(operator, id == null ? "PRODUCT_POLICY_CREATE" : "PRODUCT_POLICY_UPDATE", "CHANNEL_PRODUCT_POLICY", target.getId(), before, target, null);
        return target;
    }

    @Override
    @Transactional
    public void deletePolicy(Long id, String operator) {
        ChannelProductPolicy before = requiredPolicy(id);
        policyMapper.deleteById(id);
        logService.record(operator, "PRODUCT_POLICY_DELETE", "CHANNEL_PRODUCT_POLICY", id, before, null, null);
    }

    private MobilePlan requiredPlan(Long id) {
        MobilePlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("套餐不存在");
        }
        return plan;
    }

    private void requirePlanCode(String planCode) {
        Long count = planMapper.selectCount(new LambdaQueryWrapper<MobilePlan>()
                .eq(MobilePlan::getPlanCode, planCode));
        if (count == 0) {
            throw new IllegalArgumentException("套餐不存在");
        }
    }

    private MobilePlanOffer requiredOffer(Long id) {
        MobilePlanOffer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new IllegalArgumentException("权益不存在");
        }
        return offer;
    }

    private ChannelProductPolicy requiredPolicy(Long id) {
        ChannelProductPolicy policy = policyMapper.selectById(id);
        if (policy == null) {
            throw new IllegalArgumentException("渠道产品政策不存在");
        }
        return policy;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
