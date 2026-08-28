package com.cmhk.business.module.resource.service;

import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;

/** 推荐号码仅允许分配给学生业务订单的统一判定规则。 */
public final class ReferralEligibility {
    private ReferralEligibility() {
    }

    public static boolean isStudentOrder(MobilePlanOrder order, Customer customer) {
        return Integer.valueOf(1).equals(order.getCustomerIdentity())
                || containsStudent(order.getPlanType())
                || startsWithStudent(order.getPlanCode())
                || (customer != null && containsStudent(customer.getCustomerCategory()));
    }

    private static boolean containsStudent(String value) {
        return value != null && value.contains("学生");
    }

    private static boolean startsWithStudent(String value) {
        return value != null && value.toUpperCase().startsWith("STUDENT");
    }
}
