package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mobile_plan_offer")
public class MobilePlanOffer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planCode;

    private String offerType;

    private String offerName;

    private String offerValue;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
