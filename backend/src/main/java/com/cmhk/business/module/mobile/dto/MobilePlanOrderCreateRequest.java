package com.cmhk.business.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobilePlanOrderCreateRequest {

    @NotBlank(message = "套餐编码不能为空")
    private String planCode;

    private String customerName;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private String remark;
}

