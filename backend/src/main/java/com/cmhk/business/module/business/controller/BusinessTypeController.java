package com.cmhk.business.module.business.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.business.entity.BusinessType;
import com.cmhk.business.module.business.service.BusinessTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/business-types")
public class BusinessTypeController {

    private final BusinessTypeService businessTypeService;

    public BusinessTypeController(BusinessTypeService businessTypeService) {
        this.businessTypeService = businessTypeService;
    }

    @GetMapping
    public ApiResponse<List<BusinessType>> listBusinessTypes() {
        return ApiResponse.success(
                businessTypeService.lambdaQuery()
                        .eq(BusinessType::getEnabled, 1)
                        .orderByAsc(BusinessType::getSortOrder)
                        .list()
        );
    }
}
