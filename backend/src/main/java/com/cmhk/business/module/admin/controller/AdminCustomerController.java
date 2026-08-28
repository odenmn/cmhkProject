package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.service.AdminCustomerService;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.entity.CustomerFollowUp;
import com.cmhk.business.module.admin.dto.AdminOwnerOption;
import com.cmhk.business.module.channel.entity.Channel;
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
            @RequestParam(required=false) String type, @RequestParam(required=false) Integer status,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        List<Customer> rows = service.list(keyword, type, status, principal); log.info("管理端查询客户完成，数量={}", rows.size()); return ApiResponse.success(rows);
    }
    @GetMapping("/channels") public ApiResponse<List<Channel>> channels(
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) { return ApiResponse.success(service.channels(principal)); }
    @GetMapping("/owners") public ApiResponse<List<AdminOwnerOption>> owners() { return ApiResponse.success(service.owners()); }
    @GetMapping("/{id}/follow-ups") public ApiResponse<List<CustomerFollowUp>> followUps(
            @PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        service.detail(id, principal);
        return ApiResponse.success(service.followUps(id));
    }
    @PostMapping("/{id}/follow-ups") public ApiResponse<CustomerFollowUp> addFollowUp(
            @PathVariable Long id,
            @RequestBody CustomerFollowUp input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.addFollowUp(id, input, principal));
    }
    @GetMapping("/{id}") public ApiResponse<Map<String,Object>> detail(@PathVariable Long id,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) { return ApiResponse.success(service.detail(id, principal)); }
    @PostMapping public ApiResponse<Customer> create(@RequestBody Customer value,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) { return ApiResponse.success(service.save(null, value, principal)); }
    @PutMapping("/{id}") public ApiResponse<Customer> update(@PathVariable Long id, @RequestBody Customer value,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) { return ApiResponse.success(service.save(id, value, principal)); }
}
