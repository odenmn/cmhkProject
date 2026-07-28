package com.cmhk.business.module.business.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.business.entity.BusinessType;
import com.cmhk.business.module.business.service.BusinessTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/business-types")
public class BusinessTypeController {

    private static final Logger log = LoggerFactory.getLogger(BusinessTypeController.class);

    private final BusinessTypeService businessTypeService;

    public BusinessTypeController(BusinessTypeService businessTypeService) {
        this.businessTypeService = businessTypeService;
    }

    @GetMapping
    public ApiResponse<List<BusinessType>> listBusinessTypes() {
        log.info("开始查询启用业务类型");
        List<BusinessType> businessTypes = businessTypeService.lambdaQuery()
                .eq(BusinessType::getEnabled, 1)
                .orderByAsc(BusinessType::getSortOrder)
                .list();
        log.info("查询启用业务类型完成，数量={}", businessTypes.size());
        return ApiResponse.success(businessTypes);
    }
}
