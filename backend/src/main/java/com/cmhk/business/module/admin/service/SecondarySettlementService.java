package com.cmhk.business.module.admin.service;

import com.cmhk.business.module.admin.entity.SecondaryChannel;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRule;
import com.cmhk.business.module.admin.security.AdminPrincipal;

import java.math.BigDecimal;
import java.util.List;

/** 统一渠道佣金计算和旧渠道只读兼容服务。 */
public interface SecondarySettlementService {

    List<SecondaryChannel> channels();

    SecondaryChannel saveChannel(Long id, SecondaryChannel input, String operator);

    List<SecondaryCommissionRule> rules();

    SecondaryCommissionRule saveRule(Long id, SecondaryCommissionRule input, AdminPrincipal principal);

    List<SecondaryCommissionRecord> records(AdminPrincipal principal);

    SecondaryCommissionRecord calculate(CalculateRequest request, AdminPrincipal principal);

    SecondaryCommissionRecord adjust(Long id, BigDecimal amount, String reason, AdminPrincipal principal);

    SecondaryCommissionRecord confirm(Long id, AdminPrincipal principal);

    record CalculateRequest(
            Long orderId,
            Long channelId,
            Long ruleId,
            boolean promotionApplied,
            BigDecimal channelSubsidy,
            BigDecimal joincomSubsidy) {}
}
