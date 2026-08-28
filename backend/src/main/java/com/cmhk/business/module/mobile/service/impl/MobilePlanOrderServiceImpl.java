package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.dto.MobilePlanOrderCreateRequest;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.entity.OrderStatusCode;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.mobile.service.MobilePlanOrderService;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import com.cmhk.business.module.mobile.service.OrderStatusHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MobilePlanOrderServiceImpl extends ServiceImpl<MobilePlanOrderMapper, MobilePlanOrder> implements MobilePlanOrderService {

    private static final int CUSTOMER_IDENTITY_OVERSEAS_STUDENT = 1;

    private final MobilePlanService mobilePlanService;
    private final CustomerMapper customerMapper;
    private final OrderStatusHistoryService historyService;

    public MobilePlanOrderServiceImpl(
            MobilePlanService mobilePlanService,
            CustomerMapper customerMapper,
            OrderStatusHistoryService historyService) {
        this.mobilePlanService = mobilePlanService;
        this.customerMapper = customerMapper;
        this.historyService = historyService;
    }

    @Override
    @Transactional
    public MobilePlanOrder createTransferOrder(Long customerId, MobilePlanOrderCreateRequest request) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("登录客户不存在，请重新登录");
        }
        MobilePlan plan = mobilePlanService.lambdaQuery()
                .eq(MobilePlan::getPlanCode, request.getPlanCode())
                .eq(MobilePlan::getEnabled, 1)
                .one();

        if (plan == null) {
            throw new IllegalArgumentException("套餐不存在或已下架");
        }

        MobilePlanOrder order = new MobilePlanOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customer.getId());
        order.setPlanId(plan.getId());
        order.setPlanCode(plan.getPlanCode());
        order.setPlanName(plan.getPlanName());
        order.setPlanType(plan.getPlanType());
        order.setMonthlyFee(plan.getMonthlyFee());
        order.setChannelPriceText(plan.getChannelPriceText());
        order.setEffectiveMonthlyFee(plan.getEffectiveMonthlyFee());
        order.setEffectivePriceText(plan.getEffectivePriceText());
        order.setOfficialMonthlyFee(plan.getOfficialMonthlyFee());
        order.setOfficialPriceText(plan.getOfficialPriceText());
        order.setDataQuota(plan.getDataQuota());
        order.setVoiceQuota(plan.getVoiceQuota());
        order.setRoamingBenefit(plan.getRoamingBenefit());
        order.setContractPeriod(plan.getContractPeriod());
        order.setPromotionEndDate(plan.getPromotionEndDate());
        order.setDiscountFormula(plan.getDiscountFormula());
        order.setCustomerName(request.getCustomerName());
        order.setContactPhone(request.getContactPhone());
        order.setCustomerIdentity(request.getCustomerIdentity());
        order.setHasOffer(resolveHasOffer(request));
        order.setHasPassOrHkid(request.getHasPassOrHkid());
        order.setExpectedStartDate(request.getExpectedStartDate());
        order.setIdType(request.getIdType());
        order.setReferrerPhone(request.getReferrerPhone());
        order.setPreferredContactTime(request.getPreferredContactTime());
        order.setRemark(request.getRemark());
        order.setStatus(OrderStatusCode.FOLLOWING.name());
        order.setStatusUpdatedAt(LocalDateTime.now());
        save(order);
        historyService.record(
                order.getId(),
                "JOINCOM",
                null,
                order.getStatus(),
                "H5",
                null,
                null,
                "客户提交办理并转人工");
        return order;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "MP" + time + random;
    }

    private Integer resolveHasOffer(MobilePlanOrderCreateRequest request) {
        if (request.getCustomerIdentity() == null
                || request.getCustomerIdentity() != CUSTOMER_IDENTITY_OVERSEAS_STUDENT) {
            return 0;
        }
        return request.getHasOffer() == null ? 0 : request.getHasOffer();
    }
}
