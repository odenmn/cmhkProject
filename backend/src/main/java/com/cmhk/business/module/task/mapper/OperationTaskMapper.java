package com.cmhk.business.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.task.entity.OperationTask;
import org.apache.ibatis.annotations.Mapper;

/** 运营任务数据访问。 */
@Mapper
public interface OperationTaskMapper extends BaseMapper<OperationTask> {
}
