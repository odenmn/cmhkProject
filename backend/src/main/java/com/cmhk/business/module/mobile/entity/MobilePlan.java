package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mobile_plan")
public class MobilePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planCode;

    private String planName;

    private BigDecimal monthlyFee;

    private String dataQuota;

    private String voiceQuota;

    private String contractPeriod;

    private String description;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

