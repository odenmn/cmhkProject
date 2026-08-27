package com.cmhk.business.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.config.AdminProperties;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.mapper.AdminUserMapper;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.AdminUserService;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 管理后台用户认证与维护服务。 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper mapper;
    private final ChannelMapper channelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;
    private final OperationLogService logService;

    public AdminUserServiceImpl(
            AdminUserMapper mapper,
            ChannelMapper channelMapper,
            PasswordEncoder passwordEncoder,
            AdminProperties properties,
            OperationLogService logService) {
        this.mapper = mapper;
        this.channelMapper = channelMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.logService = logService;
    }

    /** 仅在数据库没有用户时，使用本机私有配置初始化首个管理员。 */
    @PostConstruct
    public void initializeConfiguredAdmin() {
        if (mapper.selectCount(new LambdaQueryWrapper<AdminUser>()) > 0 || blank(properties.getPassword())) {
            return;
        }
        AdminUser user = new AdminUser();
        user.setUsername(blank(properties.getUsername()) ? "admin" : properties.getUsername());
        user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        user.setDisplayName("系统管理员");
        user.setRoleCode("ADMIN");
        user.setScopeType("ALL");
        user.setStatus("ENABLED");
        mapper.insert(user);
        logService.record(user.getUsername(), "INITIALIZE_ADMIN", "ADMIN_USER", user.getId(), null, user, "从本机私有配置初始化首个管理员");
    }

    /** 校验账号密码并更新最近登录时间。 */
    public AdminUser authenticate(String username, String password) {
        AdminUser user = find(username);
        if (user == null
                || !"ENABLED".equals(user.getStatus())
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        validateRoleAndScope(user);
        user.setLastLoginAt(LocalDateTime.now());
        mapper.updateById(user);
        return user;
    }

    /** 根据数据库最新状态恢复可信身份，使停用或改权立即令旧令牌失效。 */
    public Optional<AdminPrincipal> findActivePrincipal(Long userId) {
        AdminUser user = mapper.selectById(userId);
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            return Optional.empty();
        }
        try {
            validateRoleAndScope(user);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return Optional.of(toPrincipal(user));
    }

    public List<AdminUser> list(AdminPrincipal principal) {
        requireAdmin(principal);
        return mapper.selectList(new LambdaQueryWrapper<AdminUser>().orderByDesc(AdminUser::getId));
    }

    public AdminUser create(AdminUser input, String rawPassword, AdminPrincipal principal) {
        requireAdmin(principal);
        if (blank(input.getUsername()) || blank(input.getDisplayName()) || blank(rawPassword)) {
            throw new IllegalArgumentException("账号、姓名和初始密码不能为空");
        }
        if (find(input.getUsername()) != null) {
            throw new IllegalArgumentException("管理员账号已存在");
        }
        validateRoleAndScope(input);
        input.setId(null);
        input.setPasswordHash(passwordEncoder.encode(rawPassword));
        input.setStatus("ENABLED");
        mapper.insert(input);
        logService.record(principal.username(), "CREATE_ADMIN_USER", "ADMIN_USER", input.getId(), null, input, "新增管理后台用户");
        return input;
    }

    public AdminUser update(Long id, AdminUser input, AdminPrincipal principal) {
        requireAdmin(principal);
        AdminUser before = require(id);
        validateRoleAndScope(input);
        if (id.equals(principal.userId()) && !"ADMIN".equals(input.getRoleCode())) {
            throw new IllegalArgumentException("不能降低当前登录账号的管理员权限");
        }
        input.setId(id);
        input.setUsername(before.getUsername());
        input.setPasswordHash(before.getPasswordHash());
        input.setStatus(before.getStatus());
        input.setLastLoginAt(before.getLastLoginAt());
        mapper.updateById(input);
        AdminUser after = require(id);
        logService.record(principal.username(), "UPDATE_ADMIN_USER", "ADMIN_USER", id, before, after, "修改管理后台用户资料及权限");
        return after;
    }

    public AdminUser changePassword(Long id, String password, AdminPrincipal principal) {
        requireAdmin(principal);
        if (blank(password)) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        AdminUser before = require(id);
        AdminUser update = new AdminUser();
        update.setId(id);
        update.setPasswordHash(passwordEncoder.encode(password));
        mapper.updateById(update);
        AdminUser after = require(id);
        logService.record(principal.username(), "CHANGE_ADMIN_PASSWORD", "ADMIN_USER", id, before, after, "重置管理后台用户密码");
        return after;
    }

    public AdminUser changeStatus(Long id, String status, AdminPrincipal principal) {
        requireAdmin(principal);
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("状态无效");
        }
        AdminUser before = require(id);
        if (id.equals(principal.userId()) && "DISABLED".equals(status)) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        AdminUser update = new AdminUser();
        update.setId(id);
        update.setStatus(status);
        mapper.updateById(update);
        AdminUser after = require(id);
        logService.record(principal.username(), "CHANGE_ADMIN_STATUS", "ADMIN_USER", id, before, after, "修改管理后台用户状态");
        return after;
    }

    private void validateRoleAndScope(AdminUser user) {
        if (!"ADMIN".equals(user.getRoleCode()) && !"OPERATOR".equals(user.getRoleCode())) {
            throw new IllegalArgumentException("角色仅支持ADMIN或OPERATOR");
        }
        if (!"ALL".equals(user.getScopeType())
                && !"CMHK".equals(user.getScopeType())
                && !"CHANNEL".equals(user.getScopeType())) {
            throw new IllegalArgumentException("数据范围无效");
        }
        if ("ADMIN".equals(user.getRoleCode()) && !"ALL".equals(user.getScopeType())) {
            throw new IllegalArgumentException("管理员的数据范围必须为ALL");
        }
        if ("CHANNEL".equals(user.getScopeType())) {
            if (user.getScopeId() == null || channelMapper.selectById(user.getScopeId()) == null) {
                throw new IllegalArgumentException("渠道数据范围必须选择有效渠道");
            }
        } else {
            user.setScopeId(null);
        }
    }

    private AdminPrincipal toPrincipal(AdminUser user) {
        return new AdminPrincipal(user.getId(), user.getUsername(), user.getRoleCode(), user.getScopeType(), user.getScopeId());
    }

    private void requireAdmin(AdminPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw new IllegalArgumentException("仅管理员可以执行此操作");
        }
    }

    private AdminUser find(String username) {
        return mapper.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
    }

    private AdminUser require(Long id) {
        AdminUser value = mapper.selectById(id);
        if (value == null) {
            throw new IllegalArgumentException("管理后台用户不存在");
        }
        return value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
