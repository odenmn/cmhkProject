package com.cmhk.business.module.cashback;

import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.cashback.entity.CustomerCashbackPlan;
import com.cmhk.business.module.cashback.entity.CustomerCashbackRule;
import com.cmhk.business.module.cashback.entity.CustomerCashbackInstallment;
import com.cmhk.business.module.cashback.mapper.CustomerCashbackInstallmentMapper;
import com.cmhk.business.module.cashback.mapper.CustomerCashbackPlanMapper;
import com.cmhk.business.module.cashback.mapper.CustomerCashbackRuleMapper;
import com.cmhk.business.module.cashback.service.impl.CashbackServiceImpl;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CashbackServiceTests {

    /** 旧订单套餐快照可唯一匹配时先生成待激活计划，不提前生成期次。 */
    @Test
    void generatesPendingPlanFromLegacyPlanSnapshot() {
        CustomerCashbackRuleMapper ruleMapper = mock(CustomerCashbackRuleMapper.class);
        CustomerCashbackPlanMapper planMapper = mock(CustomerCashbackPlanMapper.class);
        CustomerCashbackInstallmentMapper installmentMapper = mock(CustomerCashbackInstallmentMapper.class);
        MobilePlanOrderMapper orderMapper = mock(MobilePlanOrderMapper.class);
        MobilePlanMapper mobilePlanMapper = mock(MobilePlanMapper.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        CacheClient cacheClient = mock(CacheClient.class);
        CustomerCashbackRule rule = new CustomerCashbackRule();
        rule.setId(1L);
        rule.setPlanId(7L);
        rule.setContractMonths(12);
        rule.setInstallmentAmount(new BigDecimal("16"));
        MobilePlanOrder order = new MobilePlanOrder();
        order.setId(102L);
        order.setCustomerId(202L);
        order.setPlanName("Slash 5G 30GB");
        order.setStatus("WAITING_ACTIVATION");
        Customer customer = new Customer();
        customer.setId(202L);
        MobilePlan mobilePlan = new MobilePlan();
        mobilePlan.setId(7L);
        mobilePlan.setPlanName("学生 Slash 30GB");
        when(orderMapper.selectById(102L)).thenReturn(order);
        when(planMapper.selectOne(any())).thenReturn(null);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));
        when(mobilePlanMapper.selectById(7L)).thenReturn(mobilePlan);
        when(customerMapper.selectById(202L)).thenReturn(customer);
        doAnswer(invocation -> {
            CustomerCashbackPlan plan = invocation.getArgument(0);
            plan.setId(12L);
            return 1;
        }).when(planMapper).insert(any(CustomerCashbackPlan.class));
        CashbackServiceImpl service = new CashbackServiceImpl(
                ruleMapper,
                planMapper,
                installmentMapper,
                orderMapper,
                mobilePlanMapper,
                customerMapper,
                channelMapper,
                logService,
                cacheClient,
                new ObjectMapper());

        CustomerCashbackPlan result = service.generateForOrder(
                102L,
                new AdminPrincipal(1L, "admin", "ADMIN", "ALL", null));

        assertEquals("PENDING_ACTIVATION", result.getStatus());
        assertEquals(new BigDecimal("192.00"), result.getTotalAmount());
        assertNull(result.getActivatedAt());
        verify(installmentMapper, never()).insert(any(CustomerCashbackInstallment.class));
    }

    /** 非返现套餐激活时不生成计划，也不能阻塞订单状态更新。 */
    @Test
    void ignoresActivatedOrderWithoutMatchingRule() {
        CustomerCashbackRuleMapper ruleMapper = mock(CustomerCashbackRuleMapper.class);
        CustomerCashbackPlanMapper planMapper = mock(CustomerCashbackPlanMapper.class);
        CustomerCashbackInstallmentMapper installmentMapper = mock(CustomerCashbackInstallmentMapper.class);
        MobilePlanOrderMapper orderMapper = mock(MobilePlanOrderMapper.class);
        MobilePlanMapper mobilePlanMapper = mock(MobilePlanMapper.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        CacheClient cacheClient = mock(CacheClient.class);
        MobilePlanOrder order = new MobilePlanOrder();
        order.setId(101L);
        order.setPlanId(99L);
        order.setStatus("ACTIVATED");
        order.setContractPeriod("12个月");
        order.setActivatedAt(LocalDateTime.of(2026, 8, 28, 10, 30));
        when(planMapper.selectOne(any())).thenReturn(null);
        when(ruleMapper.selectList(any())).thenReturn(List.of());
        CashbackServiceImpl service = new CashbackServiceImpl(
                ruleMapper,
                planMapper,
                installmentMapper,
                orderMapper,
                mobilePlanMapper,
                customerMapper,
                channelMapper,
                logService,
                cacheClient,
                new ObjectMapper());

        assertDoesNotThrow(() -> service.ensurePlanForOrder(
                order,
                new AdminPrincipal(1L, "admin", "ADMIN", "ALL", null)));
    }

    /** 激活当日起满一个月生成首期，并按12期保存不可变规则快照。 */
    @Test
    void generatesTwelveMonthlyInstallmentsFromActivationDate() {
        CustomerCashbackRuleMapper ruleMapper = mock(CustomerCashbackRuleMapper.class);
        CustomerCashbackPlanMapper planMapper = mock(CustomerCashbackPlanMapper.class);
        CustomerCashbackInstallmentMapper installmentMapper = mock(CustomerCashbackInstallmentMapper.class);
        MobilePlanOrderMapper orderMapper = mock(MobilePlanOrderMapper.class);
        MobilePlanMapper mobilePlanMapper = mock(MobilePlanMapper.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        CacheClient cacheClient = mock(CacheClient.class);
        CustomerCashbackRule rule = new CustomerCashbackRule();
        rule.setId(1L);
        rule.setPlanId(7L);
        rule.setContractMonths(12);
        rule.setInstallmentAmount(new BigDecimal("16"));
        MobilePlanOrder order = new MobilePlanOrder();
        order.setId(100L);
        order.setCustomerId(200L);
        order.setPlanId(7L);
        order.setStatus("ACTIVATED");
        order.setContractPeriod("12个月");
        order.setActivatedAt(LocalDateTime.of(2026, 8, 28, 10, 30));
        Customer customer = new Customer();
        customer.setId(200L);
        customer.setChannelId(3L);
        MobilePlan mobilePlan = new MobilePlan();
        mobilePlan.setId(7L);
        mobilePlan.setPlanCode("STUDENT_SLASH_30GB_12M");
        mobilePlan.setPlanName("学生 Slash 30GB");
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(planMapper.selectOne(any())).thenReturn(null);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));
        when(customerMapper.selectById(200L)).thenReturn(customer);
        when(mobilePlanMapper.selectById(7L)).thenReturn(mobilePlan);
        doAnswer(invocation -> {
            CustomerCashbackPlan plan = invocation.getArgument(0);
            plan.setId(10L);
            return 1;
        }).when(planMapper).insert(any(CustomerCashbackPlan.class));
        CashbackServiceImpl service = new CashbackServiceImpl(
                ruleMapper,
                planMapper,
                installmentMapper,
                orderMapper,
                mobilePlanMapper,
                customerMapper,
                channelMapper,
                logService,
                cacheClient,
                new ObjectMapper());

        CustomerCashbackPlan result = service.generateForOrder(
                100L,
                new AdminPrincipal(1L, "admin", "ADMIN", "ALL", null));

        assertEquals(new BigDecimal("192.00"), result.getTotalAmount());
        assertEquals(12, result.getInstallmentCount());
        verify(installmentMapper, org.mockito.Mockito.times(12))
                .insert(any(CustomerCashbackInstallment.class));
    }
}
