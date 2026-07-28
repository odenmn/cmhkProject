package com.cmhk.business.module.mobile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmhk.business.module.mobile.entity.MobilePlan;

import java.util.List;

public interface MobilePlanService extends IService<MobilePlan> {

    List<MobilePlan> listEnabledPlansWithOffers();

    MobilePlan getEnabledPlanWithOffersByCode(String planCode);
}
