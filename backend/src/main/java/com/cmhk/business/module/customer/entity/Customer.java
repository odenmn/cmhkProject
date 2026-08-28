package com.cmhk.business.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private LocalDateTime phoneVerifiedAt;
    private String name;
    private String contactMethod;
    private String customerType;
    private String customerCategory;
    private Long channelId;

    private Long ownerUserId;
    private String intendedPlan;
    private String requirementSummary;
    private Integer currentStatus;
    private String sourceSystem;
    private String sourceCustomerId;
    /** 客户列表展示用的关联订单上台号码，不对应 customer 表字段。 */
    @TableField(exist = false)
    private String serviceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
