package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.entity.ReconciliationRow;
import com.cmhk.business.module.admin.entity.SecondaryCommissionRecord;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.mapper.ReconciliationRowMapper;
import com.cmhk.business.module.admin.mapper.SecondaryCommissionRecordMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.entity.CustomerStatusCode;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCustomerService {
    private final CustomerMapper customerMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final IccidInventoryMapper iccidMapper;
    private final ReconciliationRowMapper reconciliationRowMapper;
    private final SecondaryCommissionRecordMapper commissionRecordMapper;
    private final OperationLogService logService;
    private final ChannelMapper channelMapper;

    public AdminCustomerService(CustomerMapper customerMapper, MobilePlanOrderMapper orderMapper,
                                IccidInventoryMapper iccidMapper, ReconciliationRowMapper reconciliationRowMapper,
                                SecondaryCommissionRecordMapper commissionRecordMapper, OperationLogService logService,
                                ChannelMapper channelMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.iccidMapper = iccidMapper;
        this.reconciliationRowMapper = reconciliationRowMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.logService = logService;
        this.channelMapper = channelMapper;
    }

    /** 返回客户归属使用的主渠道选项，供管理端将渠道 ID 映射为名称。 */
    public List<Channel> channels() {
        return channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getEnabled, 1)
                .orderByAsc(Channel::getChannelName));
    }

    public List<Customer> list(String keyword, String type, Integer status) {
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(Customer::getName, keyword).or().like(Customer::getPhone, keyword))
                .eq(type != null && !type.isBlank(), Customer::getCustomerType, type)
                .eq(status != null, Customer::getCurrentStatus, status)
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
        result.put("orders", orders);
        result.put("iccids", iccids);
        result.put("reconciliationRows", rows);
        result.put("commissionRecords", commissions);
        return result;
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
        target.setChannelId(input.getChannelId());
        target.setIntendedPlan(input.getIntendedPlan());
        target.setRequirementSummary(input.getRequirementSummary());
        target.setCurrentStatus(input.getCurrentStatus() == null ? CustomerStatusCode.PENDING : input.getCurrentStatus());
        if (id == null) {
            target.setPhoneVerifiedAt(LocalDateTime.now());
            customerMapper.insert(target);
        } else customerMapper.updateById(target);
        logService.record(operator, id == null ? "CUSTOMER_CREATE" : "CUSTOMER_UPDATE", "CUSTOMER", target.getId(), before, target, null);
        return target;
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
