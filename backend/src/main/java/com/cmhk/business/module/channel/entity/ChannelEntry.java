package com.cmhk.business.module.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel_entry")
public class ChannelEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long channelId;
    private String entryToken;
    private String entryName;
    private LocalDateTime expiresAt;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
