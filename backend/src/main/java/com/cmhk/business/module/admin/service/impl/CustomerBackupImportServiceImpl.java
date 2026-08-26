package com.cmhk.business.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.admin.dto.CustomerBackupImportResult;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview;
import com.cmhk.business.module.admin.entity.CustomerBackupImport;
import com.cmhk.business.module.admin.entity.CustomerBackupImportRow;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.mapper.CustomerBackupImportMapper;
import com.cmhk.business.module.admin.mapper.CustomerBackupImportRowMapper;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.service.CustomerBackupImportService;
import com.cmhk.business.module.admin.service.CustomerBackupSimulationService;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.channel.entity.Channel;
import com.cmhk.business.module.channel.entity.ChannelEntry;
import com.cmhk.business.module.channel.entity.CustomerChannelBinding;
import com.cmhk.business.module.channel.mapper.ChannelEntryMapper;
import com.cmhk.business.module.channel.mapper.ChannelMapper;
import com.cmhk.business.module.channel.mapper.CustomerChannelBindingMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** CMHK 客户备份事务导入实现。 */
@Service
public class CustomerBackupImportServiceImpl implements CustomerBackupImportService {

    private static final String SOURCE_SYSTEM = "CMHK_BACKUP";
    private static final String IMPORT_STATUS = "CONFIRMED";

    private final CustomerBackupSimulationService simulationService;
    private final CustomerBackupImportMapper importMapper;
    private final CustomerBackupImportRowMapper importRowMapper;
    private final CustomerMapper customerMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final IccidInventoryMapper iccidMapper;
    private final ChannelMapper channelMapper;
    private final ChannelEntryMapper entryMapper;
    private final CustomerChannelBindingMapper bindingMapper;
    private final OperationLogService operationLogService;

    public CustomerBackupImportServiceImpl(
            CustomerBackupSimulationService simulationService,
            CustomerBackupImportMapper importMapper,
            CustomerBackupImportRowMapper importRowMapper,
            CustomerMapper customerMapper,
            MobilePlanOrderMapper orderMapper,
            IccidInventoryMapper iccidMapper,
            ChannelMapper channelMapper,
            ChannelEntryMapper entryMapper,
            CustomerChannelBindingMapper bindingMapper,
            OperationLogService operationLogService) {
        this.simulationService = simulationService;
        this.importMapper = importMapper;
        this.importRowMapper = importRowMapper;
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.iccidMapper = iccidMapper;
        this.channelMapper = channelMapper;
        this.entryMapper = entryMapper;
        this.bindingMapper = bindingMapper;
        this.operationLogService = operationLogService;
    }

    /** 校验用户预览过的文件摘要，并在一个事务内完成幂等导入。 */
    @Override
    @Transactional
    public CustomerBackupImportResult confirm(MultipartFile file, String previewHash, String operator) {
        CustomerBackupSimulationPreview preview = simulationService.simulate(file);
        if (previewHash == null || !preview.fileHash().equalsIgnoreCase(previewHash.trim())) {
            throw new IllegalArgumentException("文件与已确认的预览不一致，请重新预览后再确认");
        }
        if (importMapper.selectCount(new LambdaQueryWrapper<CustomerBackupImport>()
                .eq(CustomerBackupImport::getFileHash, preview.fileHash())) > 0) {
            throw new IllegalArgumentException("该客户备份已经确认导入，禁止重复写入");
        }

        CustomerBackupImport batch = createBatch(file, preview, operator);
        ImportCounters counters = new ImportCounters();
        Map<String, Customer> customers = importCustomers(batch.getId(), preview.customers(), counters, operator);
        Map<String, MobilePlanOrder> orders = importOrders(batch.getId(), preview.orders(), customers, counters);
        importIccids(batch.getId(), preview.iccids(), customers, orders, counters, operator);
        persistPreviewExceptions(batch.getId(), preview.exceptions(), counters);

        batch.setStatus(IMPORT_STATUS);
        batch.setCustomerCount(counters.customersCreated + counters.customersReused);
        batch.setOrderCount(counters.ordersCreated + counters.ordersReused);
        batch.setIccidCount(counters.iccidsCreated + counters.iccidsReused);
        batch.setExceptionCount(counters.exceptionCount);
        batch.setConfirmedAt(LocalDateTime.now());
        importMapper.updateById(batch);
        operationLogService.record(
                operator,
                "CUSTOMER_BACKUP_CONFIRM",
                "CUSTOMER_BACKUP_IMPORT",
                batch.getId(),
                null,
                Map.of(
                        "total", preview.summary().totalRecords(),
                        "customers", batch.getCustomerCount(),
                        "orders", batch.getOrderCount(),
                        "iccids", batch.getIccidCount(),
                        "exceptions", batch.getExceptionCount()
                ),
                file.getOriginalFilename()
        );
        return counters.toResult(batch, preview.summary().totalRecords());
    }

    private CustomerBackupImport createBatch(
            MultipartFile file,
            CustomerBackupSimulationPreview preview,
            String operator) {
        CustomerBackupImport batch = new CustomerBackupImport();
        batch.setFileName(safeFileName(file.getOriginalFilename()));
        batch.setFileHash(preview.fileHash());
        batch.setStatus("IMPORTING");
        batch.setTotalCount(preview.summary().totalRecords());
        batch.setCustomerCount(0);
        batch.setOrderCount(0);
        batch.setIccidCount(0);
        batch.setExceptionCount(0);
        batch.setOperatorName(operator);
        importMapper.insert(batch);
        return batch;
    }

    private Map<String, Customer> importCustomers(
            Long importId,
            List<CustomerBackupSimulationPreview.CustomerCandidate> candidates,
            ImportCounters counters,
            String operator) {
        Map<String, Customer> result = new LinkedHashMap<>();
        Map<String, ChannelContext> channelCache = new HashMap<>();
        for (CustomerBackupSimulationPreview.CustomerCandidate candidate : candidates) {
            Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getSourceSystem, SOURCE_SYSTEM)
                    .eq(Customer::getSourceCustomerId, candidate.sourceId()));
            if (customer == null) {
                customer = new Customer();
                customer.setName(blankToNull(candidate.name()));
                customer.setCustomerType(candidate.customerType());
                customer.setCustomerCategory(blankToNull(candidate.sourceCustomerCategory()));
                customer.setIntendedPlan(blankToNull(candidate.intendedPlan()));
                customer.setCurrentStatus(candidate.currentStatus());
                customer.setSourceSystem(SOURCE_SYSTEM);
                customer.setSourceCustomerId(candidate.sourceId());
                ChannelContext channel = resolveChannel(candidate.channelName(), channelCache);
                customer.setChannelId(channel == null ? null : channel.channel().getId());
                customerMapper.insert(customer);
                bindChannel(importId, candidate, customer, channel, counters, operator);
                counters.customersCreated++;
            } else {
                supplementCustomer(customer, candidate);
                customerMapper.updateById(customer);
                ChannelContext channel = resolveChannel(candidate.channelName(), channelCache);
                bindChannel(importId, candidate, customer, channel, counters, operator);
                counters.customersReused++;
            }
            result.put(candidate.sourceCustomerKey(), customer);
            persistSuccess(importId, candidate.sourceRowNumber(), candidate.sourceId(), customer.getId(), null, null);
        }
        return result;
    }

    private Map<String, MobilePlanOrder> importOrders(
            Long importId,
            List<CustomerBackupSimulationPreview.OrderCandidate> candidates,
            Map<String, Customer> customers,
            ImportCounters counters) {
        Map<String, MobilePlanOrder> result = new LinkedHashMap<>();
        for (CustomerBackupSimulationPreview.OrderCandidate candidate : candidates) {
            Customer customer = customers.get(candidate.sourceCustomerKey());
            if (customer == null) {
                persistException(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                        "CUSTOMER_NOT_IMPORTED", "客户未成功导入，订单已跳过", counters);
                continue;
            }
            MobilePlanOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MobilePlanOrder>()
                    .eq(MobilePlanOrder::getOrderSource, SOURCE_SYSTEM)
                    .eq(MobilePlanOrder::getSourceRecordId, candidate.sourceId()));
            if (order == null) {
                order = orderMapper.selectOne(new LambdaQueryWrapper<MobilePlanOrder>()
                        .eq(MobilePlanOrder::getOrderNo, candidate.orderNo()));
            }
            if (order != null && !Objects.equals(order.getCustomerId(), customer.getId())) {
                persistException(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                        "ORDER_CUSTOMER_CONFLICT", "已有订单属于其他客户，禁止覆盖", counters);
                continue;
            }
            if (order == null) {
                order = new MobilePlanOrder();
                order.setOrderNo(candidate.orderNo());
                order.setCustomerId(customer.getId());
                order.setCustomerName(blankToNull(customer.getName()));
                order.setPlanName(blankToNull(candidate.planName()));
                order.setStatus(candidate.status());
                order.setServiceNumber(blankToNull(candidate.serviceNumber()));
                order.setActivationStatus(activationStatus(candidate));
                order.setOrderSource(SOURCE_SYSTEM);
                order.setReconciliationStatus("待对账");
                order.setSourceRecordId(candidate.sourceId());
                order.setSourceChannelName(sourceChannelName(customer));
                order.setUmallStatus(blankToNull(candidate.umallStatus()));
                order.setOnboardDate(parseDate(candidate.onboardDate()));
                order.setCustomerIdentity(0);
                order.setHasOffer(0);
                order.setHasPassOrHkid(0);
                orderMapper.insert(order);
                counters.ordersCreated++;
            } else {
                supplementOrder(order, candidate);
                orderMapper.updateById(order);
                counters.ordersReused++;
            }
            result.put(candidate.orderNo(), order);
            persistSuccess(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                    customer.getId(), order.getId(), null);
        }
        return result;
    }

    private void importIccids(
            Long importId,
            List<CustomerBackupSimulationPreview.IccidCandidate> candidates,
            Map<String, Customer> customers,
            Map<String, MobilePlanOrder> orders,
            ImportCounters counters,
            String operator) {
        for (CustomerBackupSimulationPreview.IccidCandidate candidate : candidates) {
            Customer customer = candidate.bound() ? customers.get(candidate.sourceCustomerKey()) : null;
            MobilePlanOrder order = candidate.bound() ? orders.get(candidate.orderNo()) : null;
            if (candidate.bound() && (customer == null || order == null)) {
                persistException(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                        "ICCID_RELATION_MISSING", "客户或订单未成功导入，ICCID 绑定已跳过", counters);
                continue;
            }
            IccidInventory card = iccidMapper.selectOne(new LambdaQueryWrapper<IccidInventory>()
                    .eq(IccidInventory::getIccid, candidate.iccid()));
            if (hasIccidConflict(card, customer, order, candidate.cardType())) {
                persistException(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                        "ICCID_RELATION_CONFLICT", "已有 ICCID 的类型或绑定关系不同，禁止覆盖", counters);
                continue;
            }
            if (card == null) {
                card = new IccidInventory();
                card.setIccid(candidate.iccid());
                card.setBatchNo("CMHK-BACKUP-" + LocalDate.now());
                card.setStatus(candidate.status());
                card.setCardType(candidate.cardType());
                card.setServiceNumber(blankToNull(candidate.serviceNumber()));
                card.setSourceSystem(SOURCE_SYSTEM);
                card.setSourceRecordId(candidate.sourceId());
                card.setOperatorName(operator);
                applyBinding(card, customer, order, candidate.bound());
                iccidMapper.insert(card);
                counters.iccidsCreated++;
            } else {
                supplementCard(card, candidate, customer, order, operator);
                iccidMapper.updateById(card);
                counters.iccidsReused++;
            }
            persistSuccess(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                    customer == null ? null : customer.getId(), order == null ? null : order.getId(), card.getId());
        }
    }

    /** 已有非空关系不同即视为冲突，任何情况下都不自动改绑。 */
    private boolean hasIccidConflict(
            IccidInventory card,
            Customer customer,
            MobilePlanOrder order,
            String cardType) {
        if (card == null) {
            return false;
        }
        if (card.getCardType() != null && !cardType.equals(card.getCardType())) {
            return true;
        }
        if (customer != null && card.getCurrentCustomerId() != null
                && !Objects.equals(card.getCurrentCustomerId(), customer.getId())) {
            return true;
        }
        return order != null && card.getCurrentOrderId() != null
                && !Objects.equals(card.getCurrentOrderId(), order.getId());
    }

    private void applyBinding(IccidInventory card, Customer customer, MobilePlanOrder order, boolean bound) {
        if (!bound) {
            return;
        }
        card.setCurrentCustomerId(customer.getId());
        card.setCurrentOrderId(order.getId());
        card.setAssignedAt(LocalDateTime.now());
        card.setUsedAt(LocalDateTime.now());
    }

    private void supplementCard(
            IccidInventory card,
            CustomerBackupSimulationPreview.IccidCandidate candidate,
            Customer customer,
            MobilePlanOrder order,
            String operator) {
        if (card.getCardType() == null) {
            card.setCardType(candidate.cardType());
        }
        if (card.getSourceSystem() == null) {
            card.setSourceSystem(SOURCE_SYSTEM);
        }
        if (card.getSourceRecordId() == null) {
            card.setSourceRecordId(candidate.sourceId());
        }
        if (card.getServiceNumber() == null) {
            card.setServiceNumber(blankToNull(candidate.serviceNumber()));
        }
        if (candidate.bound() && card.getCurrentOrderId() == null && card.getCurrentCustomerId() == null) {
            card.setStatus(candidate.status());
            applyBinding(card, customer, order, true);
        }
        card.setOperatorName(operator);
    }

    private ChannelContext resolveChannel(String channelName, Map<String, ChannelContext> cache) {
        if (channelName == null || channelName.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(channelName, this::findOrCreateChannel);
    }

    private ChannelContext findOrCreateChannel(String channelName) {
        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelName, channelName));
        if (channel == null) {
            channel = new Channel();
            channel.setChannelCode("BACKUP_" + shortDigest(channelName));
            channel.setChannelName(channelName);
            channel.setElderlyMode(0);
            channel.setEnabled(1);
            channelMapper.insert(channel);
        }
        ChannelEntry entry = entryMapper.selectOne(new LambdaQueryWrapper<ChannelEntry>()
                .eq(ChannelEntry::getChannelId, channel.getId())
                .eq(ChannelEntry::getEntryToken, "CMHK-BACKUP-" + shortDigest(channelName)));
        if (entry == null) {
            entry = new ChannelEntry();
            entry.setChannelId(channel.getId());
            entry.setEntryToken("CMHK-BACKUP-" + shortDigest(channelName));
            entry.setEntryName("历史客户备份导入");
            entry.setEnabled(1);
            entryMapper.insert(entry);
        }
        return new ChannelContext(channel, entry);
    }

    private void bindChannel(
            Long importId,
            CustomerBackupSimulationPreview.CustomerCandidate candidate,
            Customer customer,
            ChannelContext channel,
            ImportCounters counters,
            String operator) {
        if (channel == null) {
            return;
        }
        CustomerChannelBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerChannelBinding>()
                .eq(CustomerChannelBinding::getCustomerId, customer.getId()));
        if (binding != null && !Objects.equals(binding.getChannelId(), channel.channel().getId())) {
            persistException(importId, candidate.sourceRowNumber(), candidate.sourceId(),
                    "CUSTOMER_CHANNEL_CONFLICT", "客户已有其他渠道绑定，禁止覆盖", counters);
            return;
        }
        if (binding == null) {
            binding = new CustomerChannelBinding();
            binding.setCustomerId(customer.getId());
            binding.setChannelId(channel.channel().getId());
            binding.setEntryId(channel.entry().getId());
            binding.setBoundAt(LocalDateTime.now());
            bindingMapper.insert(binding);
        }
        if (customer.getChannelId() == null) {
            customer.setChannelId(channel.channel().getId());
            customer.setCustomerType("CHANNEL");
            customerMapper.updateById(customer);
        }
    }

    private void supplementCustomer(
            Customer customer,
            CustomerBackupSimulationPreview.CustomerCandidate candidate) {
        if (customer.getName() == null) {
            customer.setName(blankToNull(candidate.name()));
        }
        if (customer.getIntendedPlan() == null) {
            customer.setIntendedPlan(blankToNull(candidate.intendedPlan()));
        }
        if (customer.getCustomerCategory() == null) {
            customer.setCustomerCategory(blankToNull(candidate.sourceCustomerCategory()));
        }
        if (customer.getCurrentStatus() == null) {
            customer.setCurrentStatus(candidate.currentStatus());
        }
    }

    private void supplementOrder(
            MobilePlanOrder order,
            CustomerBackupSimulationPreview.OrderCandidate candidate) {
        if (order.getPlanName() == null) {
            order.setPlanName(blankToNull(candidate.planName()));
        }
        if (order.getServiceNumber() == null) {
            order.setServiceNumber(blankToNull(candidate.serviceNumber()));
        }
        if (order.getSourceRecordId() == null) {
            order.setSourceRecordId(candidate.sourceId());
        }
        if (order.getUmallStatus() == null) {
            order.setUmallStatus(blankToNull(candidate.umallStatus()));
        }
        if (order.getOnboardDate() == null) {
            order.setOnboardDate(parseDate(candidate.onboardDate()));
        }
        if (order.getActivationStatus() == null || "已上台".equals(order.getActivationStatus())) {
            order.setActivationStatus(activationStatus(candidate));
        }
    }

    private String activationStatus(CustomerBackupSimulationPreview.OrderCandidate candidate) {
        if (candidate.status() != null && candidate.status().contains("已激活")) {
            return "已激活";
        }
        if (candidate.onboarded() || candidate.status() != null && candidate.status().contains("待激活")) {
            return "待激活";
        }
        return null;
    }

    private void persistPreviewExceptions(
            Long importId,
            List<CustomerBackupSimulationPreview.ExceptionCandidate> exceptions,
            ImportCounters counters) {
        for (CustomerBackupSimulationPreview.ExceptionCandidate exception : exceptions) {
            persistException(importId, exception.sourceRowNumber(), exception.sourceId(),
                    exception.code(), exception.message(), counters);
        }
    }

    private void persistSuccess(
            Long importId,
            int rowNumber,
            String sourceId,
            Long customerId,
            Long orderId,
            Long iccidId) {
        CustomerBackupImportRow row = new CustomerBackupImportRow();
        row.setImportId(importId);
        row.setSourceRowNumber(rowNumber);
        row.setSourceId(sourceId);
        row.setCustomerId(customerId);
        row.setOrderId(orderId);
        row.setIccidId(iccidId);
        row.setResultStatus("SUCCESS");
        importRowMapper.insert(row);
    }

    private void persistException(
            Long importId,
            int rowNumber,
            String sourceId,
            String code,
            String reason,
            ImportCounters counters) {
        CustomerBackupImportRow row = new CustomerBackupImportRow();
        row.setImportId(importId);
        row.setSourceRowNumber(rowNumber);
        row.setSourceId(sourceId);
        row.setResultStatus("EXCEPTION");
        row.setExceptionCode(code);
        row.setExceptionReason(reason);
        importRowMapper.insert(row);
        counters.exceptionCount++;
    }

    private String sourceChannelName(Customer customer) {
        if (customer.getChannelId() == null) {
            return null;
        }
        Channel channel = channelMapper.selectById(customer.getChannelId());
        return channel == null ? null : channel.getChannelName();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("上台日期格式必须为 yyyy-MM-dd");
        }
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "customer-backup.json";
        }
        return value.length() <= 255 ? value : value.substring(value.length() - 255);
    }

    private String shortDigest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16).toUpperCase();
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成稳定渠道标识", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record ChannelContext(Channel channel, ChannelEntry entry) {
    }

    private static final class ImportCounters {
        private int customersCreated;
        private int customersReused;
        private int ordersCreated;
        private int ordersReused;
        private int iccidsCreated;
        private int iccidsReused;
        private int exceptionCount;

        private CustomerBackupImportResult toResult(CustomerBackupImport batch, int totalRecords) {
            return new CustomerBackupImportResult(
                    batch.getId(),
                    batch.getStatus(),
                    totalRecords,
                    customersCreated,
                    customersReused,
                    ordersCreated,
                    ordersReused,
                    iccidsCreated,
                    iccidsReused,
                    exceptionCount
            );
        }
    }
}
