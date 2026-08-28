package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.common.cache.CacheClient;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.mapper.ReconciliationRowMapper;
import com.cmhk.business.module.admin.mapper.SecondaryCommissionRecordMapper;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.dto.AdminOwnerOption;
import com.cmhk.business.module.admin.mapper.AdminUserMapper;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.channel.entity.CustomerChannelBinding;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.entity.CustomerFollowUp;
import com.cmhk.business.module.customer.entity.CustomerStatusCode;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.customer.mapper.CustomerFollowUpMapper;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.channel.mapper.CustomerChannelBindingMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.resource.entity.ReferralNumber;
import com.cmhk.business.module.resource.mapper.ReferralNumberMapper;
import com.cmhk.business.module.task.entity.OperationTask;
import com.cmhk.business.module.task.mapper.OperationTaskMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCustomerService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(1);

    private final CustomerMapper customerMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final IccidInventoryMapper iccidMapper;
    private final ReferralNumberMapper referralNumberMapper;
    private final ReconciliationRowMapper reconciliationRowMapper;
    private final SecondaryCommissionRecordMapper commissionRecordMapper;
    private final OperationTaskMapper taskMapper;
    private final OperationLogService logService;
    private final ChannelMapper channelMapper;
    private final CustomerChannelBindingMapper bindingMapper;
    private final CustomerFollowUpMapper followUpMapper;
    private final AdminUserMapper adminUserMapper;
    private final CacheClient cacheClient;
    private final JavaType customerListType;
    private final JavaType channelListType;
    private final JavaType detailType;

    public AdminCustomerService(CustomerMapper customerMapper, MobilePlanOrderMapper orderMapper,
                                IccidInventoryMapper iccidMapper, ReferralNumberMapper referralNumberMapper,
                                ReconciliationRowMapper reconciliationRowMapper,
                                SecondaryCommissionRecordMapper commissionRecordMapper, OperationLogService logService,
                                OperationTaskMapper taskMapper,
                                ChannelMapper channelMapper, CustomerChannelBindingMapper bindingMapper,
                                CustomerFollowUpMapper followUpMapper, AdminUserMapper adminUserMapper,
                                CacheClient cacheClient, ObjectMapper objectMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.iccidMapper = iccidMapper;
        this.referralNumberMapper = referralNumberMapper;
        this.reconciliationRowMapper = reconciliationRowMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.taskMapper = taskMapper;
        this.logService = logService;
        this.channelMapper = channelMapper;
        this.bindingMapper = bindingMapper;
        this.followUpMapper = followUpMapper;
        this.adminUserMapper = adminUserMapper;
        this.cacheClient = cacheClient;
        this.customerListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, Customer.class);
        this.channelListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, Channel.class);
        this.detailType = objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, Object.class);
    }

    /** 返回客户归属使用的主渠道选项，供管理端将渠道 ID 映射为名称。 */
    public List<Channel> channels() {
        String key = cacheClient.versionedKey(AdminCacheKeys.CUSTOMER_CHANNELS, "enabled");
        return cacheClient.queryWithMutex(
                key,
                channelListType,
                this::channelsFromDatabase,
                Duration.ofMinutes(15),
                EMPTY_CACHE_TTL
        );
    }

    public List<Channel> channels(AdminPrincipal principal) {
        if (principal != null && "CHANNEL".equals(principal.scopeType())) {
            Channel channel = channelMapper.selectById(principal.scopeId());
            return channel == null ? List.of() : List.of(channel);
        }
        return channels();
    }

    private List<Channel> channelsFromDatabase() {
        return channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getEnabled, 1)
                .orderByAsc(Channel::getChannelName));
    }

    public List<Customer> list(String keyword, String type, Integer status) {
        String key = cacheClient.versionedKey(
                AdminCacheKeys.CUSTOMERS,
                "list:" + AdminCacheKeys.discriminator(keyword, type, status)
        );
        return cacheClient.queryWithMutex(
                key,
                customerListType,
                () -> listFromDatabase(keyword, type, status),
                CACHE_TTL,
                EMPTY_CACHE_TTL
        );
    }

    public List<Customer> list(String keyword, String type, Integer status, AdminPrincipal principal) {
        if (principal == null || !"CHANNEL".equals(principal.scopeType())) {
            return list(keyword, type, status);
        }
        return listFromDatabase(keyword, type, status, principal.scopeId());
    }

    private List<Customer> listFromDatabase(String keyword, String type, Integer status) {
        return listFromDatabase(keyword, type, status, null);
    }

    private List<Customer> listFromDatabase(String keyword, String type, Integer status, Long channelId) {
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(Customer::getName, keyword).or().like(Customer::getPhone, keyword))
                .eq(type != null && !type.isBlank(), Customer::getCustomerType, type)
                .eq(status != null, Customer::getCurrentStatus, status)
                .eq(channelId != null, Customer::getChannelId, channelId)
                .orderByDesc(Customer::getId));
        fillServiceNumbers(customers);
        return customers;
    }

    /** 批量补充客户最新订单的上台号码，避免客户列表逐行查询订单。 */
    private void fillServiceNumbers(List<Customer> customers) {
        if (customers.isEmpty()) {
            return;
        }
        List<Long> customerIds = customers.stream()
                .map(Customer::getId)
                .toList();
        List<MobilePlanOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .in(MobilePlanOrder::getCustomerId, customerIds)
                .isNotNull(MobilePlanOrder::getServiceNumber)
                .orderByDesc(MobilePlanOrder::getId));
        Map<Long, String> serviceNumberByCustomer = new HashMap<>();
        for (MobilePlanOrder order : orders) {
            if (order.getServiceNumber() != null && !order.getServiceNumber().isBlank()) {
                serviceNumberByCustomer.putIfAbsent(order.getCustomerId(), order.getServiceNumber());
            }
        }
        for (Customer customer : customers) {
            customer.setServiceNumber(serviceNumberByCustomer.get(customer.getId()));
        }
    }

    public Map<String, Object> detail(Long id) {
        String key = cacheClient.versionedKey(AdminCacheKeys.CUSTOMERS, "detail:" + id);
        return cacheClient.queryWithMutex(
                key,
                detailType,
                () -> detailFromDatabase(id),
                CACHE_TTL,
                EMPTY_CACHE_TTL
        );
    }

    public Map<String, Object> detail(Long id, AdminPrincipal principal) {
        Customer customer = required(id);
        requireChannelAccess(customer.getChannelId(), principal);
        Map<String, Object> detail = detail(id);
        if (principal != null && principal.isAdmin()) {
            return detail;
        }
        Map<String, Object> scopedDetail = new LinkedHashMap<>(detail);
        Object commissionValue = detail.get("commissionRecords");
        if (commissionValue instanceof List<?> records) {
            scopedDetail.put("commissionRecords", records.stream()
                    .filter(SecondaryCommissionRecord.class::isInstance)
                    .map(SecondaryCommissionRecord.class::cast)
                    .map(this::maskCommissionAmounts)
                    .toList());
        }
        return scopedDetail;
    }

    /** 客户详情对非管理员隐藏佣金金额和规则快照。 */
    private SecondaryCommissionRecord maskCommissionAmounts(SecondaryCommissionRecord source) {
        SecondaryCommissionRecord target = new SecondaryCommissionRecord();
        target.setId(source.getId());
        target.setOrderId(source.getOrderId());
        target.setChannelId(source.getChannelId());
        target.setRuleId(source.getRuleId());
        target.setPromotionApplied(source.getPromotionApplied());
        target.setStatus(source.getStatus());
        target.setConfirmedBy(source.getConfirmedBy());
        target.setConfirmedAt(source.getConfirmedAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private Map<String, Object> detailFromDatabase(Long id) {
        Customer customer = required(id);
        List<MobilePlanOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .eq(MobilePlanOrder::getCustomerId, id).orderByDesc(MobilePlanOrder::getId));
        List<Long> orderIds = orders.stream().map(MobilePlanOrder::getId).toList();
        List<IccidInventory> iccids = iccidMapper.selectList(new LambdaQueryWrapper<IccidInventory>()
                .eq(IccidInventory::getCurrentCustomerId, id));
        List<ReconciliationRow> rows = orderIds.isEmpty() ? List.of() : reconciliationRowMapper.selectList(
                new LambdaQueryWrapper<ReconciliationRow>().in(ReconciliationRow::getMatchedOrderId, orderIds));
        List<SecondaryCommissionRecord> commissions = orderIds.isEmpty() ? List.of() : commissionRecordMapper.selectList(
                new LambdaQueryWrapper<SecondaryCommissionRecord>().in(SecondaryCommissionRecord::getOrderId, orderIds));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", customer);
        result.put("channel", customer.getChannelId() == null ? null : channelMapper.selectById(customer.getChannelId()));
        result.put("owner", customer.getOwnerUserId() == null ? null : adminUserMapper.selectById(customer.getOwnerUserId()));
        result.put("orders", orders);
        result.put("iccids", iccids);
        LambdaQueryWrapper<ReferralNumber> referralQuery = new LambdaQueryWrapper<>();
        referralQuery.eq(ReferralNumber::getAssignedCustomerId, id);
        if (!orderIds.isEmpty()) {
            referralQuery.or().in(ReferralNumber::getSourceOrderId, orderIds);
        }
        result.put("referralNumbers", referralNumberMapper.selectList(referralQuery
                .orderByDesc(ReferralNumber::getId)));
        result.put("reconciliationRows", rows);
        result.put("commissionRecords", commissions);
        result.put("tasks", taskMapper.selectList(new LambdaQueryWrapper<OperationTask>()
                .eq(OperationTask::getCustomerId, id)
                .orderByDesc(OperationTask::getCreatedAt)));
        result.put("followUps", followUps(id));
        return result;
    }

    /** 返回可被选为客户负责人的内部启用用户。 */
    public List<AdminOwnerOption> owners() {
        return adminUserMapper.selectList(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getStatus, "ENABLED")
                .in(AdminUser::getRoleCode, List.of("ADMIN", "OPERATOR"))
                .orderByAsc(AdminUser::getDisplayName))
                .stream()
                .map(user -> new AdminOwnerOption(user.getId(), user.getUsername(), user.getDisplayName()))
                .toList();
    }

    public List<CustomerFollowUp> followUps(Long customerId) {
        return followUpMapper.selectList(new LambdaQueryWrapper<CustomerFollowUp>()
                .eq(CustomerFollowUp::getCustomerId, customerId)
                .orderByDesc(CustomerFollowUp::getCreatedAt)
                .orderByDesc(CustomerFollowUp::getId));
    }

    /** 新增客户跟进时校验数据范围，并记录实际操作人。 */
    @Transactional
    public CustomerFollowUp addFollowUp(
            Long customerId,
            CustomerFollowUp input,
            AdminPrincipal principal) {
        Customer customer = required(customerId);
        requireChannelAccess(customer.getChannelId(), principal);
        if (input.getContent() == null || input.getContent().isBlank()) {
            throw new IllegalArgumentException("跟进内容不能为空");
        }
        CustomerFollowUp target = new CustomerFollowUp();
        target.setCustomerId(customerId);
        target.setFollowUpType(input.getFollowUpType() == null ? "GENERAL" : input.getFollowUpType());
        target.setContent(input.getContent().trim());
        target.setNextFollowUpAt(input.getNextFollowUpAt());
        target.setOperatorUserId(principal.userId());
        target.setOperatorName(principal.username());
        followUpMapper.insert(target);
        logService.record(
                principal.username(),
                "CUSTOMER_FOLLOW_UP_CREATE",
                "CUSTOMER",
                customerId,
                null,
                Map.of("followUpId", target.getId()),
                null);
        cacheClient.invalidateNamespacesAfterCommit(AdminCacheKeys.CUSTOMERS);
        return target;
    }

    @Transactional
    public Customer save(Long id, Customer input, String operator) {
        Customer before = id == null ? null : required(id);
        Customer target = id == null ? new Customer() : copy(before);
        target.setName(input.getName());
        target.setPhone(requiredText(input.getPhone(), "手机号不能为空"));
        target.setContactMethod(input.getContactMethod());
        target.setCustomerType(input.getCustomerType() == null ? "DIRECT" : input.getCustomerType());
        target.setCustomerCategory(input.getCustomerCategory());
        Channel selectedChannel = requireChannel(input.getChannelId());
        target.setChannelId(selectedChannel.getId());
        target.setOwnerUserId(input.getOwnerUserId());
        target.setIntendedPlan(input.getIntendedPlan());
        target.setRequirementSummary(input.getRequirementSummary());
        target.setCurrentStatus(input.getCurrentStatus() == null ? CustomerStatusCode.PENDING : input.getCurrentStatus());
        if (id == null) {
            target.setPhoneVerifiedAt(LocalDateTime.now());
            customerMapper.insert(target);
        } else {
            customerMapper.updateById(target);
        }
        syncBinding(target.getId(), selectedChannel.getId());
        logService.record(operator, id == null ? "CUSTOMER_CREATE" : "CUSTOMER_UPDATE", "CUSTOMER", target.getId(), before, target, null);
        cacheClient.invalidateNamespacesAfterCommit(
                AdminCacheKeys.CUSTOMERS,
                AdminCacheKeys.ICCIDS,
                AdminCacheKeys.DASHBOARD
        );
        return target;
    }

    public Customer save(Long id, Customer input, AdminPrincipal principal) {
        if (id != null) {
            requireChannelAccess(required(id).getChannelId(), principal);
        }
        requireChannelAccess(input.getChannelId(), principal);
        return save(id, input, principal.username());
    }

    /** 绑定表是业务事实，后台修改渠道时必须同步更新绑定记录。 */
    private void syncBinding(Long customerId, Long channelId) {
        CustomerChannelBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<CustomerChannelBinding>()
                        .eq(CustomerChannelBinding::getCustomerId, customerId));
        if (binding == null) {
            binding = new CustomerChannelBinding();
            binding.setCustomerId(customerId);
            binding.setChannelId(channelId);
            binding.setBoundAt(LocalDateTime.now());
            bindingMapper.insert(binding);
            return;
        }
        if (!channelId.equals(binding.getChannelId())) {
            binding.setChannelId(channelId);
            binding.setEntryId(null);
            bindingMapper.updateById(binding);
        }
    }

    private Channel requireChannel(Long channelId) {
        if (channelId == null) {
            throw new IllegalArgumentException("请选择有效渠道");
        }
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("请选择有效渠道");
        }
        return channel;
    }

    private void requireChannelAccess(Long channelId, AdminPrincipal principal) {
        if (principal != null
                && "CHANNEL".equals(principal.scopeType())
                && !principal.scopeId().equals(channelId)) {
            throw new IllegalArgumentException("当前账号不能访问其他渠道数据");
        }
    }

    private Customer required(Long id) {
        Customer value = customerMapper.selectById(id);
        if (value == null) throw new IllegalArgumentException("客户不存在");
        return value;
    }

    private Customer copy(Customer source) {
        Customer copy = new Customer();
        copy.setId(source.getId()); copy.setPhone(source.getPhone()); copy.setPhoneVerifiedAt(source.getPhoneVerifiedAt());
        copy.setName(source.getName()); copy.setContactMethod(source.getContactMethod()); copy.setCustomerType(source.getCustomerType());
        copy.setCustomerCategory(source.getCustomerCategory());
        copy.setChannelId(source.getChannelId()); copy.setIntendedPlan(source.getIntendedPlan());
        copy.setOwnerUserId(source.getOwnerUserId());
        copy.setRequirementSummary(source.getRequirementSummary()); copy.setCurrentStatus(source.getCurrentStatus());
        copy.setSourceSystem(source.getSourceSystem()); copy.setSourceCustomerId(source.getSourceCustomerId());
        copy.setServiceNumber(source.getServiceNumber());
        copy.setCreatedAt(source.getCreatedAt()); copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
