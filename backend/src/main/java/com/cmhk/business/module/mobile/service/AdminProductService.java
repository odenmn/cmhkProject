package com.cmhk.business.module.mobile.service;

import com.cmhk.business.module.mobile.entity.ChannelProductPolicy;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOffer;

import java.util.List;
import java.util.Map;

/** 管理端套餐、权益和渠道产品政策维护接口。 */
public interface AdminProductService {

    List<MobilePlan> listPlans();

    MobilePlan savePlan(Long id, MobilePlan input, String operator);

    void disablePlan(Long id, String operator);

    List<MobilePlanOffer> listOffers(String planCode);

    MobilePlanOffer saveOffer(Long id, MobilePlanOffer input, String operator);

    void deleteOffer(Long id, String operator);

    List<Map<String, Object>> listPolicies();

    ChannelProductPolicy savePolicy(Long id, ChannelProductPolicy input, String operator);

    void deletePolicy(Long id, String operator);
}
