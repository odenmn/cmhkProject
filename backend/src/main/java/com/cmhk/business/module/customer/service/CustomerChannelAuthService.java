package com.cmhk.business.module.customer.service;

import com.cmhk.business.module.channel.dto.ChannelEntryContextResponse;
import com.cmhk.business.module.customer.dto.PhoneLoginRequest;
import com.cmhk.business.module.customer.dto.PhoneLoginResponse;
import com.cmhk.business.module.customer.dto.VerificationCodeSendRequest;

/** 协调渠道入口校验与客户手机号登录的应用服务。 */
public interface CustomerChannelAuthService {
    ChannelEntryContextResponse resolveEntry(String entryToken);

    void sendMockVerificationCode(VerificationCodeSendRequest request);

    PhoneLoginResponse loginByPhone(PhoneLoginRequest request);
}
