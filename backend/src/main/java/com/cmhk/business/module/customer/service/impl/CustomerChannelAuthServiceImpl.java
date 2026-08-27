package com.cmhk.business.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.channel.dto.ChannelEntryContextResponse;
import com.cmhk.business.module.customer.dto.PhoneLoginRequest;
import com.cmhk.business.module.customer.dto.PhoneLoginResponse;
import com.cmhk.business.module.customer.dto.VerificationCodeSendRequest;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.entity.ChannelEntry;
import com.cmhk.business.module.channel.entity.CustomerChannelBinding;
import com.cmhk.business.module.channel.mapper.ChannelEntryMapper;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.channel.mapper.CustomerChannelBindingMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.entity.PhoneVerificationCode;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.customer.mapper.PhoneVerificationCodeMapper;
import com.cmhk.business.module.customer.security.AccessTokenService;
import com.cmhk.business.module.customer.service.CustomerChannelAuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
/** 客户登录流程实现：验证入口、验证码、客户档案和首次渠道绑定。 */
public class CustomerChannelAuthServiceImpl implements CustomerChannelAuthService {

    private static final String MOCK_CODE = "123456";
    private static final String CODE_STATUS_SENT = "SENT";
    private static final String CODE_STATUS_USED = "USED";
    private static final String CODE_STATUS_INVALID = "INVALID";

    private final ChannelMapper channelMapper;
    private final ChannelEntryMapper channelEntryMapper;
    private final CustomerMapper customerMapper;
    private final CustomerChannelBindingMapper customerChannelBindingMapper;
    private final PhoneVerificationCodeMapper phoneVerificationCodeMapper;
    private final AccessTokenService accessTokenService;

    public CustomerChannelAuthServiceImpl(ChannelMapper channelMapper, ChannelEntryMapper channelEntryMapper,
                                  CustomerMapper customerMapper, CustomerChannelBindingMapper customerChannelBindingMapper,
                                  PhoneVerificationCodeMapper phoneVerificationCodeMapper,
                                  AccessTokenService accessTokenService) {
        this.channelMapper = channelMapper;
        this.channelEntryMapper = channelEntryMapper;
        this.customerMapper = customerMapper;
        this.customerChannelBindingMapper = customerChannelBindingMapper;
        this.phoneVerificationCodeMapper = phoneVerificationCodeMapper;
        this.accessTokenService = accessTokenService;
    }

    @Override
    public ChannelEntryContextResponse resolveEntry(String entryToken) {
        EntryContext context = findEntryContext(entryToken);
        return new ChannelEntryContextResponse(context.entry().getEntryToken(), context.entry().getEntryName(),
                context.channel().getChannelName(), context.channel().getElderlyMode());
    }

    @Override
    public void sendMockVerificationCode(VerificationCodeSendRequest request) {
        findEntryContext(request.getEntryToken());
        String phone = normalizePhone(request.getPhone());
        PhoneVerificationCode latestCode = phoneVerificationCodeMapper.selectOne(
                new LambdaQueryWrapper<PhoneVerificationCode>()
                        .eq(PhoneVerificationCode::getPhone, phone)
                        .orderByDesc(PhoneVerificationCode::getCreatedAt)
                        .last("LIMIT 1"));
        if (latestCode != null && latestCode.getCreatedAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("验证码已发送，请 60 秒后再试");
        }
        PhoneVerificationCode code = new PhoneVerificationCode();
        code.setPhone(phone);
        code.setCodeHash(hash(MOCK_CODE));
        code.setAttemptCount(0);
        code.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        code.setStatus(CODE_STATUS_SENT);
        phoneVerificationCodeMapper.insert(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PhoneLoginResponse loginByPhone(PhoneLoginRequest request) {
        EntryContext context = findEntryContext(request.getEntryToken());
        String phone = normalizePhone(request.getPhone());
        verifyCode(phone, request.getVerificationCode());

        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone));
        boolean newCustomer = customer == null;
        if (newCustomer) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setPhoneVerifiedAt(LocalDateTime.now());
            customerMapper.insert(customer);
        } else {
            customer.setPhoneVerifiedAt(LocalDateTime.now());
            customerMapper.updateById(customer);
        }

        CustomerChannelBinding binding = customerChannelBindingMapper.selectOne(
                new LambdaQueryWrapper<CustomerChannelBinding>().eq(CustomerChannelBinding::getCustomerId, customer.getId()));
        Channel boundChannel = context.channel();
        if (binding == null) {
            binding = new CustomerChannelBinding();
            binding.setCustomerId(customer.getId());
            binding.setChannelId(context.channel().getId());
            binding.setEntryId(context.entry().getId());
            binding.setBoundAt(LocalDateTime.now());
            customerChannelBindingMapper.insert(binding);
        } else {
            Channel existingBoundChannel = channelMapper.selectById(binding.getChannelId());
            if (existingBoundChannel != null) {
                boundChannel = existingBoundChannel;
            }
        }

        // 绑定表是客户渠道归属的业务事实，客户主表只保存同步后的冗余渠道ID。
        if (!boundChannel.getId().equals(customer.getChannelId())) {
            customer.setChannelId(boundChannel.getId());
            customerMapper.updateById(customer);
        }

        AccessTokenService.IssuedAccessToken token = accessTokenService.issue(customer.getId());
        return new PhoneLoginResponse(customer.getId(), newCustomer, boundChannel.getChannelName(),
                boundChannel.getElderlyMode(), token.value(), token.expiresAt().toString());
    }

    private EntryContext findEntryContext(String entryToken) {
        ChannelEntry entry = channelEntryMapper.selectOne(new LambdaQueryWrapper<ChannelEntry>()
                .eq(ChannelEntry::getEntryToken, entryToken)
                .eq(ChannelEntry::getEnabled, 1));
        if (entry == null || (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new IllegalArgumentException("渠道入口不存在、已失效或已关闭");
        }
        Channel channel = channelMapper.selectById(entry.getChannelId());
        if (channel == null || channel.getEnabled() == null || channel.getEnabled() != 1) {
            throw new IllegalArgumentException("渠道不可用");
        }
        return new EntryContext(entry, channel);
    }

    private void verifyCode(String phone, String verificationCode) {
        PhoneVerificationCode code = phoneVerificationCodeMapper.selectOne(new LambdaQueryWrapper<PhoneVerificationCode>()
                .eq(PhoneVerificationCode::getPhone, phone)
                .eq(PhoneVerificationCode::getStatus, CODE_STATUS_SENT)
                .orderByDesc(PhoneVerificationCode::getCreatedAt)
                .last("LIMIT 1"));
        if (code == null || code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("验证码不存在或已过期");
        }
        if (!hash(verificationCode).equals(code.getCodeHash())) {
            code.setAttemptCount(code.getAttemptCount() + 1);
            if (code.getAttemptCount() >= 5) {
                code.setStatus(CODE_STATUS_INVALID);
            }
            phoneVerificationCodeMapper.updateById(code);
            throw new IllegalArgumentException("验证码错误");
        }
        code.setStatus(CODE_STATUS_USED);
        code.setUsedAt(LocalDateTime.now());
        phoneVerificationCodeMapper.updateById(code);
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[ -]", "");
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private record EntryContext(ChannelEntry entry, Channel channel) {
    }
}
