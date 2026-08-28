package com.cmhk.business.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 运营人员处理业务异常和跟进事项的任务主记录。 */
@Data
@TableName("operation_task")
public class OperationTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String taskType;
    private String title;
    private String taskStatus;
    private String priority;
    private Long customerId;
    private Long orderId;
    private Long channelId;
    private String sourceType;
    private String sourceRecordId;
    private String dedupKey;
    private String openDedupKey;
    private Long assigneeUserId;
    private String assigneeName;
    private LocalDateTime dueAt;
    private String handlingResult;
    private Long createdByUserId;
    private String createdByName;
    private LocalDateTime claimedAt;
    private LocalDateTime completedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
