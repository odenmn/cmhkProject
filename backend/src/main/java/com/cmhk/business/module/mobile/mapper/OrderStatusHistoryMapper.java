package com.cmhk.business.module.mobile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.mobile.entity.OrderStatusHistory;
import org.apache.ibatis.annotations.Mapper;

/** 订单状态历史数据访问。 */
@Mapper
public interface OrderStatusHistoryMapper extends BaseMapper<OrderStatusHistory> {
}
