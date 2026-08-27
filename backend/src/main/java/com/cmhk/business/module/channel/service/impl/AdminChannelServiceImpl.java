package com.cmhk.business.module.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.admin.mapper.AdminUserMapper;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.channel.service.AdminChannelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** 统一渠道档案的数据库实现。 */
@Service
public class AdminChannelServiceImpl implements AdminChannelService {

    private static final Set<String> CHANNEL_TYPES = Set.of(
            "RESOURCE",
            "ORGANIZATION",
            "ENTERPRISE",
            "SALES_AGENT");
    private static final Set<String> COOPERATION_STATUSES = Set.of(
            "PENDING",
            "ACTIVE",
            "SUSPENDED",
            "ENDED");

    private final ChannelMapper channelMapper;
    private final AdminUserMapper adminUserMapper;
    private final OperationLogService logService;

    public AdminChannelServiceImpl(
            ChannelMapper channelMapper,
            AdminUserMapper adminUserMapper,
            OperationLogService logService) {
        this.channelMapper = channelMapper;
        this.adminUserMapper = adminUserMapper;
        this.logService = logService;
    }

    @Override
    public List<Channel> list(AdminPrincipal principal) {
        LambdaQueryWrapper<Channel> query = new LambdaQueryWrapper<Channel>()
                .orderByAsc(Channel::getChannelName)
                .orderByAsc(Channel::getId);
        if (principal != null && "CHANNEL".equals(principal.scopeType())) {
            query.eq(Channel::getId, principal.scopeId());
        }
        return channelMapper.selectList(query);
    }

    @Override
    @Transactional
    public Channel save(Long id, Channel input, AdminPrincipal principal) {
        requireAdmin(principal);
        validate(id, input);
        Channel before = id == null ? null : require(id);
        if (id != null) {
            input.setChannelCode(before.getChannelCode());
            input.setElderlyMode(before.getElderlyMode());
            input.setWechatServiceUrl(before.getWechatServiceUrl());
            input.setWechatQrCodeUrl(before.getWechatQrCodeUrl());
        }
        input.setId(id);
        input.setEnabled("ACTIVE".equals(input.getCooperationStatus()) ? 1 : 0);
        if (input.getElderlyMode() == null) {
            input.setElderlyMode(0);
        }
        if (id == null) {
            channelMapper.insert(input);
        } else {
            channelMapper.updateById(input);
        }
        logService.record(
                principal.username(),
                id == null ? "CHANNEL_CREATE" : "CHANNEL_UPDATE",
                "CHANNEL",
                input.getId(),
                before,
                input,
                "维护统一渠道档案");
        return require(input.getId());
    }

    private void validate(Long id, Channel input) {
        if (blank(input.getChannelCode()) || blank(input.getChannelName())) {
            throw new IllegalArgumentException("渠道编码和渠道名称不能为空");
        }
        if (!CHANNEL_TYPES.contains(input.getChannelType())) {
            throw new IllegalArgumentException("渠道类型无效");
        }
        if (!COOPERATION_STATUSES.contains(input.getCooperationStatus())) {
            throw new IllegalArgumentException("合作状态无效");
        }
        Channel sameCode = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, input.getChannelCode()));
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new IllegalArgumentException("渠道编码已存在");
        }
        if (input.getOwnerUserId() != null && adminUserMapper.selectById(input.getOwnerUserId()) == null) {
            throw new IllegalArgumentException("渠道负责人不存在");
        }
        validateParent(id, input.getParentChannelId());
    }

    /** 沿父级链向上检查，避免渠道层级形成循环。 */
    private void validateParent(Long id, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(id)) {
            throw new IllegalArgumentException("渠道不能以自身作为上级");
        }
        Channel parent = require(parentId);
        int depth = 0;
        while (parent.getParentChannelId() != null && depth < 100) {
            if (parent.getParentChannelId().equals(id)) {
                throw new IllegalArgumentException("渠道层级不能形成循环");
            }
            parent = require(parent.getParentChannelId());
            depth++;
        }
        if (depth >= 100) {
            throw new IllegalArgumentException("渠道层级异常，请检查历史数据");
        }
    }

    private Channel require(Long id) {
        Channel value = channelMapper.selectById(id);
        if (value == null) {
            throw new IllegalArgumentException("渠道不存在");
        }
        return value;
    }

    private void requireAdmin(AdminPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw new IllegalArgumentException("仅管理员可以维护渠道档案");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
