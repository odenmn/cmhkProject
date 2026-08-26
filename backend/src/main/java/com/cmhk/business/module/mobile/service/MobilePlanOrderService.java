package com.cmhk.business.module.mobile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmhk.business.module.mobile.dto.MobilePlanOrderCreateRequest;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;

public interface MobilePlanOrderService extends IService<MobilePlanOrder> {

    MobilePlanOrder createTransferOrder(Long customerId, MobilePlanOrderCreateRequest request);
}
