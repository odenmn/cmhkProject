package com.cmhk.business.module.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 渠道可推广套餐及其结算规则引用。 */
@Data
@TableName("channel_product_policy")
public class ChannelProductPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    private Long planId;

    private Integer promotable;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String cashbackRuleRef;

    private String commissionRuleRef;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
