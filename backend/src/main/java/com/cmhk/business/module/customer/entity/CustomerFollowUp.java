package com.cmhk.business.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 客户人工跟进记录，只保存业务沟通摘要，不保存正式身份材料。 */
@Data
@TableName("customer_follow_up")
public class CustomerFollowUp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String followUpType;

    private String content;

    private LocalDateTime nextFollowUpAt;

    private Long operatorUserId;

    private String operatorName;

    private LocalDateTime createdAt;
}
