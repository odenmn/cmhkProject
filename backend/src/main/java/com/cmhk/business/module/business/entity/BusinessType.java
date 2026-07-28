package com.cmhk.business.module.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_type")
public class BusinessType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

