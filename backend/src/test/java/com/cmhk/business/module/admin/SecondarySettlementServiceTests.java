package com.cmhk.business.module.admin;

import com.cmhk.business.module.admin.entity.*;
import com.cmhk.business.module.admin.mapper.*;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.admin.service.SecondarySettlementService;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecondarySettlementServiceTests {
    @Test void calculatesAndSnapshotsTnCommissionWithBigDecimal() {
        SecondaryChannelMapper channels=mock(SecondaryChannelMapper.class);SecondaryCommissionRuleMapper rules=mock(SecondaryCommissionRuleMapper.class);SecondaryCommissionRecordMapper records=mock(SecondaryCommissionRecordMapper.class);MobilePlanOrderMapper orders=mock(MobilePlanOrderMapper.class);OperationLogService logs=mock(OperationLogService.class);
        SecondaryChannel channel=new SecondaryChannel();channel.setId(2L);SecondaryCommissionRule rule=new SecondaryCommissionRule();rule.setId(3L);rule.setRuleName("24月套餐");rule.setMonthlyFee(new BigDecimal("100"));rule.setContractMonths(24);rule.setMainMultiplier(new BigDecimal("4"));rule.setExtraMultiplier(new BigDecimal("0.5"));rule.setPromotionMultiplier(new BigDecimal("0.5"));rule.setChannelMultiplier(new BigDecimal("2"));rule.setDefaultChannelSubsidy(new BigDecimal("20"));rule.setDefaultJoincomSubsidy(new BigDecimal("10"));MobilePlanOrder order=new MobilePlanOrder();order.setId(1L);order.setStatus("已激活");order.setReconciliationStatus("已对账");
        when(channels.selectById(2L)).thenReturn(channel);when(rules.selectById(3L)).thenReturn(rule);when(orders.selectById(1L)).thenReturn(order);when(records.selectCount(any())).thenReturn(0L);
        SecondarySettlementService service=new SecondarySettlementService(channels,rules,records,orders,logs,new ObjectMapper());
        var result=service.calculate(new SecondarySettlementService.CalculateRequest(1L,2L,3L,true,null,null),"admin");
        assertEquals(new BigDecimal("500.00"),result.getJoincomTotal());assertEquals(new BigDecimal("180.00"),result.getChannelPayable());assertEquals(new BigDecimal("290.00"),result.getJoincomRetained());assertEquals(new BigDecimal("100.00"),result.getT1Amount());assertEquals(new BigDecimal("350.00"),result.getT3Amount());assertEquals(new BigDecimal("50.00"),result.getT7Amount());assertEquals(new BigDecimal("180.00"),result.getFinalAmount());
        verify(records).insert(any(SecondaryCommissionRecord.class));
    }
}
