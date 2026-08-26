package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.config.AdminProperties;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.mapper.AdminUserMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminUserService {
    private final AdminUserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;
    private final OperationLogService logService;
    public AdminUserService(AdminUserMapper mapper, PasswordEncoder passwordEncoder, AdminProperties properties, OperationLogService logService) { this.mapper=mapper; this.passwordEncoder=passwordEncoder; this.properties=properties; this.logService=logService; }

    @PostConstruct
    public void initializeConfiguredAdmin() {
        if (mapper.selectCount(new LambdaQueryWrapper<AdminUser>()) > 0 || blank(properties.getPassword())) return;
        AdminUser user = new AdminUser(); user.setUsername(blank(properties.getUsername()) ? "admin" : properties.getUsername()); user.setPasswordHash(passwordEncoder.encode(properties.getPassword())); user.setDisplayName("系统管理员"); user.setRoleCode("ADMIN"); user.setStatus("ENABLED"); mapper.insert(user);
        logService.record(user.getUsername(), "INITIALIZE_ADMIN", "ADMIN_USER", user.getId(), null, user, "从本机私有配置初始化首个管理员");
    }

    public AdminUser authenticate(String username, String password) {
        AdminUser user = find(username);
        if (user == null || !"ENABLED".equals(user.getStatus()) || !passwordEncoder.matches(password, user.getPasswordHash())) throw new IllegalArgumentException("管理员账号或密码错误");
        user.setLastLoginAt(LocalDateTime.now()); mapper.updateById(user); return user;
    }
    public List<AdminUser> list() { return mapper.selectList(new LambdaQueryWrapper<AdminUser>().orderByDesc(AdminUser::getId)); }
    public AdminUser create(AdminUser input, String rawPassword, String operator) {
        if (blank(input.getUsername()) || blank(input.getDisplayName()) || blank(rawPassword)) throw new IllegalArgumentException("账号、姓名和初始密码不能为空");
        if (find(input.getUsername()) != null) throw new IllegalArgumentException("管理员账号已存在");
        input.setId(null); input.setPasswordHash(passwordEncoder.encode(rawPassword)); input.setRoleCode("ADMIN"); input.setStatus("ENABLED"); mapper.insert(input); logService.record(operator,"CREATE_ADMIN_USER","ADMIN_USER",input.getId(),null,input,"新增管理员"); return input;
    }
    public AdminUser update(Long id, AdminUser input, String operator) {
        AdminUser before=require(id); input.setId(id); input.setUsername(before.getUsername()); input.setPasswordHash(before.getPasswordHash()); input.setRoleCode("ADMIN"); input.setLastLoginAt(before.getLastLoginAt()); mapper.updateById(input); AdminUser after=require(id); logService.record(operator,"UPDATE_ADMIN_USER","ADMIN_USER",id,before,after,"修改管理员资料"); return after;
    }
    public AdminUser changePassword(Long id, String password, String operator) { if(blank(password)) throw new IllegalArgumentException("新密码不能为空"); AdminUser before=require(id); AdminUser update=new AdminUser(); update.setId(id); update.setPasswordHash(passwordEncoder.encode(password)); mapper.updateById(update); AdminUser after=require(id); logService.record(operator,"CHANGE_ADMIN_PASSWORD","ADMIN_USER",id,before,after,"重置管理员密码"); return after; }
    public AdminUser changeStatus(Long id, String status, String operator) { if(!"ENABLED".equals(status)&&!"DISABLED".equals(status)) throw new IllegalArgumentException("状态无效"); AdminUser before=require(id); if(before.getUsername().equals(operator)&&"DISABLED".equals(status)) throw new IllegalArgumentException("不能停用当前登录账号"); AdminUser update=new AdminUser();update.setId(id);update.setStatus(status);mapper.updateById(update);AdminUser after=require(id);logService.record(operator,"CHANGE_ADMIN_STATUS","ADMIN_USER",id,before,after,"修改管理员状态");return after; }
    private AdminUser find(String username){return mapper.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername,username));}
    private AdminUser require(Long id){AdminUser v=mapper.selectById(id);if(v==null)throw new IllegalArgumentException("管理员不存在");return v;}
    private boolean blank(String value){return value==null||value.isBlank();}
}
