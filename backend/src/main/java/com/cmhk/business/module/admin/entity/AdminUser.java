package com.cmhk.business.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

/** JOINCOM管理后台内部用户。 */
@Data
@TableName("admin_user")
public class AdminUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @TableField("password_hash")
    @JsonIgnore
    private String passwordHash;
    private String displayName;
    private String phone;
    private String email;
    private String roleCode;
    private String scopeType;
    private Long scopeId;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
