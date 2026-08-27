package com.cmhk.business.module.channel.service;

import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.channel.entity.Channel;

import java.util.List;

/** 管理端统一渠道主档服务。 */
public interface AdminChannelService {

    List<Channel> list(AdminPrincipal principal);

    Channel save(Long id, Channel input, AdminPrincipal principal);
}
