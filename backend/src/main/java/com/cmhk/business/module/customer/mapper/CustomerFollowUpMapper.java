package com.cmhk.business.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.customer.entity.CustomerFollowUp;
import org.apache.ibatis.annotations.Mapper;

/** 客户跟进记录数据访问层。 */
@Mapper
public interface CustomerFollowUpMapper extends BaseMapper<CustomerFollowUp> {
}
