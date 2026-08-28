package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.mobile.entity.OrderStatusHistory;
import com.cmhk.business.module.mobile.mapper.OrderStatusHistoryMapper;
import com.cmhk.business.module.mobile.service.OrderStatusHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** 订单状态历史服务实现，状态未变化时不产生重复记录。 */
@Service
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {

    private final OrderStatusHistoryMapper mapper;

    public OrderStatusHistoryServiceImpl(OrderStatusHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(
            Long orderId,
            String statusType,
            String beforeStatus,
            String afterStatus,
            String sourceType,
            Long operatorUserId,
            String operatorName,
            String remark) {
        if (afterStatus == null || Objects.equals(beforeStatus, afterStatus)) {
            return;
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setStatusType(statusType);
        history.setBeforeStatus(beforeStatus);
        history.setAfterStatus(afterStatus);
        history.setSourceType(sourceType);
        history.setOperatorUserId(operatorUserId);
        history.setOperatorName(operatorName);
        history.setRemark(remark);
        mapper.insert(history);
    }

    @Override
    public List<OrderStatusHistory> listByOrderId(Long orderId) {
        return mapper.selectList(new LambdaQueryWrapper<OrderStatusHistory>()
                .eq(OrderStatusHistory::getOrderId, orderId)
                .orderByDesc(OrderStatusHistory::getCreatedAt)
                .orderByDesc(OrderStatusHistory::getId));
    }
}
