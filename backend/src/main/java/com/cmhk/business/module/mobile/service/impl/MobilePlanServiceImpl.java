package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOffer;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOfferMapper;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MobilePlanServiceImpl extends ServiceImpl<MobilePlanMapper, MobilePlan> implements MobilePlanService {

    private static final String ENABLED_PLAN_LIST_CACHE_KEY = "cmhk:mobile-plan:list:enabled";
    private static final Duration PLAN_LIST_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration EMPTY_PLAN_LIST_CACHE_TTL = Duration.ofMinutes(2);

    private final MobilePlanOfferMapper mobilePlanOfferMapper;
    private final CacheClient cacheClient;
    private final JavaType mobilePlanListType;

    public MobilePlanServiceImpl(
            MobilePlanOfferMapper mobilePlanOfferMapper,
            CacheClient cacheClient,
            ObjectMapper objectMapper
    ) {
        this.mobilePlanOfferMapper = mobilePlanOfferMapper;
        this.cacheClient = cacheClient;
        this.mobilePlanListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, MobilePlan.class);
    }

    @Override
    public List<MobilePlan> listEnabledPlansWithOffers() {
        return cacheClient.queryWithMutex(
                ENABLED_PLAN_LIST_CACHE_KEY,
                mobilePlanListType,
                this::listEnabledPlansWithOffersFromDb,
                PLAN_LIST_CACHE_TTL,
                EMPTY_PLAN_LIST_CACHE_TTL
        );
    }

    @Override
    public MobilePlan getEnabledPlanWithOffersByCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return null;
        }
        return listEnabledPlansWithOffers().stream()
                .filter(plan -> planCode.equals(plan.getPlanCode()))
                .findFirst()
                .orElse(null);
    }

    private List<MobilePlan> listEnabledPlansWithOffersFromDb() {
        List<MobilePlan> plans = lambdaQuery()
                .eq(MobilePlan::getEnabled, 1)
                .orderByAsc(MobilePlan::getSortOrder)
                .list();

        if (plans.isEmpty()) {
            return plans;
        }

        List<String> planCodes = plans.stream()
                .map(MobilePlan::getPlanCode)
                .toList();

        List<MobilePlanOffer> offers = mobilePlanOfferMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<MobilePlanOffer>lambdaQuery()
                        .in(MobilePlanOffer::getPlanCode, planCodes)
                        .eq(MobilePlanOffer::getEnabled, 1)
                        .orderByAsc(MobilePlanOffer::getSortOrder)
        );

        Map<String, List<MobilePlanOffer>> offersByPlanCode = offers.stream()
                .collect(Collectors.groupingBy(MobilePlanOffer::getPlanCode));

        plans.forEach(plan -> plan.setOffers(
                offersByPlanCode.getOrDefault(plan.getPlanCode(), Collections.emptyList())
        ));

        return plans;
    }
}
