package com.cmhk.business.module.resource;

import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.resource.service.ReferralEligibility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralEligibilityTests {
    @Test
    void shouldAcceptEverySupportedStudentMarker() {
        MobilePlanOrder identityOrder = new MobilePlanOrder();
        identityOrder.setCustomerIdentity(1);
        assertThat(ReferralEligibility.isStudentOrder(identityOrder, null)).isTrue();

        MobilePlanOrder planTypeOrder = new MobilePlanOrder();
        planTypeOrder.setPlanType("留学生套餐");
        assertThat(ReferralEligibility.isStudentOrder(planTypeOrder, null)).isTrue();

        MobilePlanOrder planCodeOrder = new MobilePlanOrder();
        planCodeOrder.setPlanCode("student-30gb");
        assertThat(ReferralEligibility.isStudentOrder(planCodeOrder, null)).isTrue();

        Customer studentCustomer = new Customer();
        studentCustomer.setCustomerCategory("留学生");
        assertThat(ReferralEligibility.isStudentOrder(new MobilePlanOrder(), studentCustomer)).isTrue();
    }

    @Test
    void shouldRejectOrdinaryOrder() {
        MobilePlanOrder order = new MobilePlanOrder();
        order.setCustomerIdentity(0);
        order.setPlanType("普通月费");
        order.setPlanCode("GENERAL-01");
        Customer customer = new Customer();
        customer.setCustomerCategory("普通客户");
        assertThat(ReferralEligibility.isStudentOrder(order, customer)).isFalse();
    }
}
