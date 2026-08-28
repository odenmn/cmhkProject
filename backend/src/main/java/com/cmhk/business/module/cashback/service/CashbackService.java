package com.cmhk.business.module.cashback.service;

import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.cashback.entity.CustomerCashbackInstallment;
import com.cmhk.business.module.cashback.entity.CustomerCashbackPlan;
import com.cmhk.business.module.cashback.entity.CustomerCashbackRule;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;

import java.util.List;
import java.util.Map;

/** 管理客户返现规则、计划和期次，不接入返现易外部事实。 */
public interface CashbackService {

    List<CustomerCashbackRule> rules();

    CustomerCashbackRule saveRule(Long id, CustomerCashbackRule input, AdminPrincipal principal);

    List<Map<String, Object>> plans(PlanQuery query, AdminPrincipal principal);

    List<CustomerCashbackInstallment> installments(Long cashbackPlanId, AdminPrincipal principal);

    CustomerCashbackPlan generateForOrder(Long orderId, AdminPrincipal principal);

    Map<String, Object> generateForExistingOrders(AdminPrincipal principal);

    CustomerCashbackInstallment confirmInstallment(
            Long installmentId,
            String remark,
            AdminPrincipal principal);

    void ensurePlanForOrder(MobilePlanOrder order, AdminPrincipal principal);

    record PlanQuery(Long customerId, Long orderId, Long channelId, String status) {
    }
}
