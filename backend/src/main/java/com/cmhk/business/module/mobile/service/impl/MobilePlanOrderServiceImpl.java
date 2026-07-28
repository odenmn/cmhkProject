package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmhk.business.module.mobile.dto.MobilePlanOrderCreateRequest;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.mobile.service.MobilePlanOrderService;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MobilePlanOrderServiceImpl extends ServiceImpl<MobilePlanOrderMapper, MobilePlanOrder> implements MobilePlanOrderService {

    private static final String STATUS_TRANSFER_TO_AGENT = "TRANSFER_TO_AGENT";

    private final MobilePlanService mobilePlanService;

    public MobilePlanOrderServiceImpl(MobilePlanService mobilePlanService) {
        this.mobilePlanService = mobilePlanService;
    }

    @Override
    public MobilePlanOrder createTransferOrder(MobilePlanOrderCreateRequest request) {
        MobilePlan plan = mobilePlanService.lambdaQuery()
                .eq(MobilePlan::getPlanCode, request.getPlanCode())
                .eq(MobilePlan::getEnabled, 1)
                .one();

        if (plan == null) {
            throw new IllegalArgumentException("套餐不存在或已下架");
        }

        MobilePlanOrder order = new MobilePlanOrder();
        order.setOrderNo(generateOrderNo());
        order.setPlanCode(plan.getPlanCode());
        order.setPlanName(plan.getPlanName());
        order.setMonthlyFee(plan.getMonthlyFee());
        order.setCustomerName(request.getCustomerName());
        order.setContactPhone(request.getContactPhone());
        order.setRemark(request.getRemark());
        order.setStatus(STATUS_TRANSFER_TO_AGENT);
        save(order);
        return order;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "MP" + time + random;
    }
}

