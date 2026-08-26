package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.service.AdminCustomerService;
import com.cmhk.business.module.customer.entity.Customer;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.Map;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {
    private static final Logger log = LoggerFactory.getLogger(AdminCustomerController.class);
    private final AdminCustomerService service;
    public AdminCustomerController(AdminCustomerService service) { this.service = service; }

    @GetMapping public ApiResponse<List<Customer>> list(@RequestParam(required=false) String keyword,
            @RequestParam(required=false) String type, @RequestParam(required=false) String status) {
        List<Customer> rows = service.list(keyword, type, status); log.info("管理端查询客户完成，数量={}", rows.size()); return ApiResponse.success(rows);
    }
    @GetMapping("/{id}") public ApiResponse<Map<String,Object>> detail(@PathVariable Long id) { return ApiResponse.success(service.detail(id)); }
    @PostMapping public ApiResponse<Customer> create(@RequestBody Customer value,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) { return ApiResponse.success(service.save(null, value, operator)); }
    @PutMapping("/{id}") public ApiResponse<Customer> update(@PathVariable Long id, @RequestBody Customer value,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator) { return ApiResponse.success(service.save(id, value, operator)); }
}
