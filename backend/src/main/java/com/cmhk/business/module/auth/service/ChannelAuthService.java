package com.cmhk.business.module.auth.service;

import com.cmhk.business.module.auth.dto.ChannelEntryContextResponse;
import com.cmhk.business.module.auth.dto.PhoneLoginRequest;
import com.cmhk.business.module.auth.dto.PhoneLoginResponse;
import com.cmhk.business.module.auth.dto.VerificationCodeSendRequest;

public interface ChannelAuthService {
    ChannelEntryContextResponse resolveEntry(String entryToken);

    void sendMockVerificationCode(VerificationCodeSendRequest request);

    PhoneLoginResponse loginByPhone(PhoneLoginRequest request);
}
