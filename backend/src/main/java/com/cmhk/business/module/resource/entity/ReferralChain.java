package com.cmhk.business.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 一条独立的推荐号码接龙。 */
@Data
@TableName("referral_chain")
public class ReferralChain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainCode;
    private String chainName;
    private String status;
    private Long currentHeadNumberId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
