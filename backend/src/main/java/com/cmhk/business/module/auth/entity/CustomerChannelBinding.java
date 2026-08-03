package com.cmhk.business.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_channel_binding")
public class CustomerChannelBinding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long channelId;
    private Long entryId;
    private LocalDateTime boundAt;
    private LocalDateTime createdAt;
}
