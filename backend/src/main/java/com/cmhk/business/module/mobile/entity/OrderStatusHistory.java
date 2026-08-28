package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 订单各类状态的不可变变更留痕。 */
@Data
@TableName("order_status_history")
public class OrderStatusHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String statusType;
    private String beforeStatus;
    private String afterStatus;
    private String sourceType;
    private Long operatorUserId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
}
