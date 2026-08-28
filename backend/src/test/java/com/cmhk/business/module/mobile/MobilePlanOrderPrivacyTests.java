package com.cmhk.business.module.mobile;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.cashback.service.CashbackService;
import com.cmhk.business.module.mobile.controller.MobilePlanController;
import com.cmhk.business.module.mobile.dto.MobilePlanOrderCreateRequest;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import com.cmhk.business.module.mobile.service.OrderStatusHistoryService;
import com.cmhk.business.module.mobile.service.impl.MobilePlanOrderServiceImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 P0 隐私边界：订单不再接收、写入或返回证件号码。
 */
class MobilePlanOrderPrivacyTests {

    @Test
    void createRequestDoesNotExposeIdNoProperty() {
        assertThrows(NoSuchFieldException.class,
                () -> MobilePlanOrderCreateRequest.class.getDeclaredField("idNo"));
    }

    @Test
    void orderResponseDoesNotSerializeHistoricalIdNo() throws Exception {
        MobilePlanOrder order = new MobilePlanOrder();
        order.setOrderNo("TEST-ORDER");
        order.setIdNo("historical-sensitive-value");

        String json = new ObjectMapper().writeValueAsString(order);

        assertFalse(json.contains("idNo"));
        assertFalse(json.contains("historical-sensitive-value"));
    }

    @Test
    void legacyJsonIdNoIsIgnoredDuringCompatibilityDeserialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MobilePlanOrderCreateRequest request = objectMapper.readValue(
                "{\"planCode\":\"PLAN-001\",\"idNo\":\"legacy-sensitive-value\"}",
                MobilePlanOrderCreateRequest.class);
        MobilePlanOrder adminInput = objectMapper.readValue(
                "{\"orderNo\":\"TEST-ORDER\",\"idNo\":\"legacy-sensitive-value\"}",
                MobilePlanOrder.class);

        assertFalse(request.getPlanCode().isBlank());
        assertNull(adminInput.getIdNo());
    }

    @Test
    void legacyCreateRequestSucceedsWithoutReturningIdNo() throws Exception {
        MobilePlanService mobilePlanService = mock(MobilePlanService.class);
        com.cmhk.business.module.mobile.service.MobilePlanOrderService orderService =
                mock(com.cmhk.business.module.mobile.service.MobilePlanOrderService.class);
        MobilePlanOrder responseOrder = new MobilePlanOrder();
        responseOrder.setOrderNo("TEST-ORDER");
        responseOrder.setIdNo("historical-sensitive-value");
        when(orderService.createTransferOrder(any(), any())).thenReturn(responseOrder);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new MobilePlanController(mobilePlanService, orderService)).build();

        mockMvc.perform(post("/api/mobile-plans/orders")
                        .requestAttr("authenticatedCustomerId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PLAN-001",
                                  "contactPhone": "10000000000",
                                  "customerIdentity": 0,
                                  "hasPassOrHkid": 0,
                                  "idNo": "legacy-sensitive-value"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("TEST-ORDER"))
                .andExpect(jsonPath("$.data.idNo").doesNotExist());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createTransferOrderDoesNotWriteIdNo() {
        MobilePlanService mobilePlanService = mock(MobilePlanService.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        MobilePlanOrderMapper orderMapper = mock(MobilePlanOrderMapper.class);
        OrderStatusHistoryService historyService = mock(OrderStatusHistoryService.class);
        CashbackService cashbackService = mock(CashbackService.class);
        LambdaQueryChainWrapper<MobilePlan> query = mock(LambdaQueryChainWrapper.class);

        Customer customer = new Customer();
        customer.setId(10L);
        MobilePlan plan = new MobilePlan();
        plan.setId(20L);
        plan.setPlanCode("PLAN-001");
        plan.setPlanName("测试套餐");
        plan.setMonthlyFee(new BigDecimal("100.00"));

        when(customerMapper.selectById(10L)).thenReturn(customer);
        when(mobilePlanService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(SFunction.class), any())).thenReturn(query);
        when(query.one()).thenReturn(plan);
        when(orderMapper.insert(any(MobilePlanOrder.class))).thenReturn(1);

        MobilePlanOrderServiceImpl service = new MobilePlanOrderServiceImpl(
                mobilePlanService,
                customerMapper,
                historyService,
                cashbackService);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);

        MobilePlanOrderCreateRequest request = new MobilePlanOrderCreateRequest();
        request.setPlanCode("PLAN-001");
        request.setContactPhone("10000000000");
        request.setCustomerIdentity(0);
        request.setHasPassOrHkid(0);

        MobilePlanOrder created = service.createTransferOrder(10L, request);

        assertNull(created.getIdNo());
        verify(orderMapper).insert(created);
    }
}
