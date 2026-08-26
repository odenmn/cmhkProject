package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("secondary_channel")
public class SecondaryChannel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelCode;
    private String channelName;
    private String contactName;
    private String contactPhone;
    private String settlementInfo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
