package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOffer;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOfferMapper;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MobilePlanServiceImpl extends ServiceImpl<MobilePlanMapper, MobilePlan> implements MobilePlanService {

    private final MobilePlanOfferMapper mobilePlanOfferMapper;

    public MobilePlanServiceImpl(MobilePlanOfferMapper mobilePlanOfferMapper) {
        this.mobilePlanOfferMapper = mobilePlanOfferMapper;
    }

    @Override
    public List<MobilePlan> listEnabledPlansWithOffers() {
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
