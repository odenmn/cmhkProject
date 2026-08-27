package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.service.AdminChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端统一渠道档案接口。 */
@RestController
@RequestMapping("/api/admin/channels")
public class AdminChannelController {

    private static final Logger log = LoggerFactory.getLogger(AdminChannelController.class);

    private final AdminChannelService service;

    public AdminChannelController(AdminChannelService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Channel>> list(
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        List<Channel> rows = service.list(principal);
        log.info("管理端查询统一渠道档案完成，数量={}", rows.size());
        return ApiResponse.success(rows);
    }

    @PostMapping
    public ApiResponse<Channel> create(
            @RequestBody Channel input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.save(null, input, principal));
    }

    @PutMapping("/{id}")
    public ApiResponse<Channel> update(
            @PathVariable Long id,
            @RequestBody Channel input,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL) AdminPrincipal principal) {
        return ApiResponse.success(service.save(id, input, principal));
    }
}
