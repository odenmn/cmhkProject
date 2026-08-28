package com.cmhk.business.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.task.entity.OperationTaskHistory;
import org.apache.ibatis.annotations.Mapper;

/** 运营任务处理历史数据访问。 */
@Mapper
public interface OperationTaskHistoryMapper extends BaseMapper<OperationTaskHistory> {
}
