package com.cmhk.business.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private String intendedPlan;
    private String requirementSummary;
    private Integer currentStatus;
    private String sourceSystem;
    private String sourceCustomerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
