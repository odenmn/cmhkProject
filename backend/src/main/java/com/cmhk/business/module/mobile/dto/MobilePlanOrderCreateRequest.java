package com.cmhk.business.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MobilePlanOrderCreateRequest {

    @NotBlank(message = "套餐编码不能为空")
    private String planCode;

    private String customerName;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @NotNull(message = "客户身份不能为空")
    @Min(value = 0, message = "客户身份不正确")
    @Max(value = 1, message = "客户身份不正确")
    private Integer customerIdentity;

    @Min(value = 0, message = "offer 状态不正确")
    @Max(value = 1, message = "offer 状态不正确")
    private Integer hasOffer;

    @NotNull(message = "请选择目前是否有通行证或 HKID")
    @Min(value = 0, message = "通行证或 HKID 状态不正确")
    @Max(value = 1, message = "通行证或 HKID 状态不正确")
    private Integer hasPassOrHkid;

    private LocalDate expectedStartDate;

    private String idType;

    private String idNo;

    private String referrerPhone;

    private String preferredContactTime;

    private String remark;
}
