package com.cmhk.business.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 任务领取、转派和处理等动作的不可变历史记录。 */
@Data
@TableName("operation_task_history")
public class OperationTaskHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private Long beforeAssigneeUserId;
    private Long afterAssigneeUserId;
    private Long operatorUserId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
}
