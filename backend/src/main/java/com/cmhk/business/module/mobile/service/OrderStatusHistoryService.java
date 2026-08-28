package com.cmhk.business.module.mobile.service;

import com.cmhk.business.module.mobile.entity.OrderStatusHistory;

import java.util.List;

/** 统一记录和查询订单状态变化。 */
public interface OrderStatusHistoryService {

    void record(
            Long orderId,
            String statusType,
            String beforeStatus,
            String afterStatus,
            String sourceType,
            Long operatorUserId,
            String operatorName,
            String remark);

    List<OrderStatusHistory> listByOrderId(Long orderId);
}
