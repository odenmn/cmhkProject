package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.config.AdminAuthInterceptor;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.service.AdminUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service){this.service=service;}
    @GetMapping public ApiResponse<List<AdminUser>> list(){return ApiResponse.success(service.list());}
    @PostMapping public ApiResponse<AdminUser> create(@RequestBody CreateRequest request,@RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator){AdminUser user=new AdminUser();user.setUsername(request.username());user.setDisplayName(request.displayName());user.setPhone(request.phone());user.setEmail(request.email());return ApiResponse.success(service.create(user,request.password(),operator));}
    @PutMapping("/{id}") public ApiResponse<AdminUser> update(@PathVariable Long id,@RequestBody UpdateRequest request,@RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator){AdminUser user=new AdminUser();user.setDisplayName(request.displayName());user.setPhone(request.phone());user.setEmail(request.email());return ApiResponse.success(service.update(id,user,operator));}
    @PostMapping("/{id}/password") public ApiResponse<AdminUser> password(@PathVariable Long id,@RequestBody PasswordRequest request,@RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator){return ApiResponse.success(service.changePassword(id,request.password(),operator));}
    @PostMapping("/{id}/status") public ApiResponse<AdminUser> status(@PathVariable Long id,@RequestBody StatusRequest request,@RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator){return ApiResponse.success(service.changeStatus(id,request.status(),operator));}
    public record CreateRequest(@NotBlank String username,@NotBlank String displayName,String phone,String email,@NotBlank String password){}
    public record UpdateRequest(@NotBlank String displayName,String phone,String email){}
    public record PasswordRequest(@NotBlank String password){}
    public record StatusRequest(@NotBlank String status){}
}
