package com.cmhk.business.module.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** JOINCOM统一渠道主档。 */
@Data
@TableName("channel")
public class Channel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelCode;
    private String channelName;
    private String channelType;
    private Long parentChannelId;
    private String contactName;
    private String contactPhone;
    private String cooperationStatus;
    private String settlementInfo;
    private Long ownerUserId;
    private Integer elderlyMode;
    private String wechatServiceUrl;
    private String wechatQrCodeUrl;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
