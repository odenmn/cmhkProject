package com.cmhk.business.module.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.service.OperationLogService;
import com.cmhk.business.module.admin.service.TabularFileReader;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import com.cmhk.business.module.resource.entity.ReferralChain;
import com.cmhk.business.module.resource.entity.ReferralNumber;
import com.cmhk.business.module.resource.entity.ReferralNumberHistory;
import com.cmhk.business.module.resource.mapper.ReferralChainMapper;
import com.cmhk.business.module.resource.mapper.ReferralNumberHistoryMapper;
import com.cmhk.business.module.resource.mapper.ReferralNumberMapper;
import com.cmhk.business.module.resource.service.ReferralNumberService;
import com.cmhk.business.module.resource.service.ReferralEligibility;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HexFormat;

/** 通过数据库行锁和唯一索引保证多条接龙并发分配安全。 */
@Service
public class ReferralNumberServiceImpl implements ReferralNumberService {
    private static final String ACTIVE = "ACTIVE";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String RESERVED = "RESERVED";
    private static final String USED = "USED";
    private static final String DISABLED = "DISABLED";
    private static final Set<String> CHAIN_STATUSES = Set.of(ACTIVE, "PAUSED", "CLOSED");
    private static final Set<String> ONBOARDED_STATUSES = Set.of("WAITING_ACTIVATION", "ACTIVATED", "COMPLETED");

    private final ReferralChainMapper chainMapper;
    private final ReferralNumberMapper numberMapper;
    private final ReferralNumberHistoryMapper historyMapper;
    private final MobilePlanOrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final IccidInventoryMapper iccidMapper;
    private final OperationLogService logService;
    private final TabularFileReader fileReader;

    public ReferralNumberServiceImpl(
            ReferralChainMapper chainMapper,
            ReferralNumberMapper numberMapper,
            ReferralNumberHistoryMapper historyMapper,
            MobilePlanOrderMapper orderMapper,
            CustomerMapper customerMapper,
            IccidInventoryMapper iccidMapper,
            OperationLogService logService,
            TabularFileReader fileReader) {
        this.chainMapper = chainMapper;
        this.numberMapper = numberMapper;
        this.historyMapper = historyMapper;
        this.orderMapper = orderMapper;
        this.customerMapper = customerMapper;
        this.iccidMapper = iccidMapper;
        this.logService = logService;
        this.fileReader = fileReader;
    }

    @Override
    public List<Map<String, Object>> chains() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ReferralChain chain : chainMapper.selectList(new LambdaQueryWrapper<ReferralChain>()
                .orderByDesc(ReferralChain::getId))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("chain", chain);
            item.put("head", chain.getCurrentHeadNumberId() == null
                    ? null
                    : numberMapper.selectById(chain.getCurrentHeadNumberId()));
            item.put("numberCount", numberMapper.selectCount(new LambdaQueryWrapper<ReferralNumber>()
                    .eq(ReferralNumber::getChainId, chain.getId())));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> numbers(Long chainId, String status, String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<ReferralNumber> rows = numberMapper.selectList(new LambdaQueryWrapper<ReferralNumber>()
                .eq(chainId != null, ReferralNumber::getChainId, chainId)
                .eq(hasText(status), ReferralNumber::getStatus, status)
                .like(hasText(keyword), ReferralNumber::getReferralNumber, keyword)
                .orderByDesc(ReferralNumber::getId));
        for (ReferralNumber row : rows) {
            MobilePlanOrder order = row.getAssignedOrderId() == null
                    ? null
                    : orderMapper.selectById(row.getAssignedOrderId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("number", row);
            item.put("orderNo", order == null ? null : order.getOrderNo());
            item.put("customerName", order == null ? null : order.getCustomerName());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<MobilePlanOrder> eligibleOrders() {
        List<MobilePlanOrder> result = new ArrayList<>();
        for (MobilePlanOrder order : orderMapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .orderByDesc(MobilePlanOrder::getId))) {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            long allocationCount = numberMapper.selectCount(new LambdaQueryWrapper<ReferralNumber>()
                    .eq(ReferralNumber::getAssignedOrderId, order.getId()));
            if (ReferralEligibility.isStudentOrder(order, customer) && allocationCount == 0) {
                result.add(order);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public ReferralChain createChain(
            String code,
            String name,
            String remark,
            AdminPrincipal principal) {
        ReferralChain chain = new ReferralChain();
        chain.setChainCode(required(code, "接龙编码不能为空"));
        chain.setChainName(required(name, "接龙名称不能为空"));
        chain.setStatus(ACTIVE);
        chain.setOperatorName(principal.username());
        chain.setRemark(remark);
        try {
            chainMapper.insert(chain);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("接龙编码已存在");
        }
        logService.record(principal.username(), "REFERRAL_CHAIN_CREATE", "REFERRAL_CHAIN", chain.getId(), null, chain, remark);
        return chain;
    }

    @Override
    @Transactional
    public ReferralChain changeChainStatus(
            Long chainId,
            String status,
            String reason,
            AdminPrincipal principal) {
        String targetStatus = required(status, "接龙状态不能为空").toUpperCase();
        if (!CHAIN_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException("不支持的接龙状态");
        }
        ReferralChain chain = requiredChainForUpdate(chainId);
        ReferralChain before = copyChain(chain);
        chain.setStatus(targetStatus);
        chain.setOperatorName(principal.username());
        chain.setRemark(reason);
        chainMapper.updateById(chain);
        logService.record(principal.username(), "REFERRAL_CHAIN_STATUS", "REFERRAL_CHAIN", chainId, before, chain, reason);
        return chain;
    }

    @Override
    @Transactional
    public ReferralNumber addCandidate(
            Long chainId,
            String number,
            String sourceReference,
            AdminPrincipal principal) {
        requiredChainForUpdate(chainId);
        ReferralNumber candidate = new ReferralNumber();
        candidate.setChainId(chainId);
        candidate.setReferralNumber(normalizeNumber(number));
        candidate.setStatus(DISABLED);
        candidate.setSourceType("CONFIRMED_IMPORT");
        candidate.setSourceReference(sourceReference);
        candidate.setOperatorName(principal.username());
        candidate.setRemark("候选号码，指定为龙头后才可分配");
        try {
            numberMapper.insert(candidate);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("推荐号码已属于其他接龙");
        }
        recordHistory(candidate, "IMPORT", null, null, principal, candidate.getRemark());
        logService.record(principal.username(), "REFERRAL_NUMBER_IMPORT", "REFERRAL_NUMBER", candidate.getId(), null, candidate, sourceReference);
        return candidate;
    }

    @Override
    public Map<String, Object> previewImport(Long chainId, MultipartFile file) {
        if (chainMapper.selectById(chainId) == null) {
            throw new IllegalArgumentException("接龙不存在");
        }
        List<Map<String, String>> source = fileReader.read(file);
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int valid = 0;
        int conflict = 0;
        for (int index = 0; index < source.size(); index++) {
            String raw = firstValue(source.get(index));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowNumber", index + 1);
            row.put("referralNumber", raw);
            String error = validateImportNumber(raw, seen);
            row.put("error", error);
            if (error == null) {
                valid++;
            } else {
                conflict++;
            }
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileHash", fileHash(file));
        result.put("total", source.size());
        result.put("valid", valid);
        result.put("conflict", conflict);
        result.put("rows", rows);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmImport(
            Long chainId,
            MultipartFile file,
            String expectedHash,
            AdminPrincipal principal) {
        String actualHash = fileHash(file);
        if (!actualHash.equalsIgnoreCase(required(expectedHash, "预览摘要不能为空"))) {
            throw new IllegalArgumentException("文件内容与已确认预览不一致");
        }
        requiredChainForUpdate(chainId);
        List<Map<String, String>> source = fileReader.read(file);
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> conflicts = new ArrayList<>();
        int success = 0;
        for (int index = 0; index < source.size(); index++) {
            String raw = firstValue(source.get(index));
            String error = validateImportNumber(raw, seen);
            if (error != null) {
                conflicts.add(Map.of("rowNumber", index + 1, "referralNumber", raw == null ? "" : raw, "error", error));
                continue;
            }
            ReferralNumber candidate = new ReferralNumber();
            candidate.setChainId(chainId);
            candidate.setReferralNumber(normalizeNumber(raw));
            candidate.setStatus(DISABLED);
            candidate.setSourceType("CONFIRMED_IMPORT");
            candidate.setSourceReference(actualHash);
            candidate.setOperatorName(principal.username());
            candidate.setRemark("已确认导入，指定为龙头后才可分配");
            numberMapper.insert(candidate);
            recordHistory(candidate, "IMPORT", null, null, principal, candidate.getRemark());
            success++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", source.size());
        result.put("success", success);
        result.put("conflicts", conflicts);
        logService.record(principal.username(), "REFERRAL_IMPORT_CONFIRM", "REFERRAL_CHAIN", chainId, null, result, actualHash);
        return result;
    }

    @Override
    @Transactional
    public ReferralNumber designateHead(
            Long chainId,
            Long numberId,
            String reason,
            AdminPrincipal principal) {
        ReferralChain chain = requiredChainForUpdate(chainId);
        requireActive(chain);
        ReferralNumber target = requiredNumberForUpdate(numberId);
        if (!chainId.equals(target.getChainId())) {
            throw new IllegalArgumentException("推荐号码不属于当前接龙");
        }
        if (RESERVED.equals(target.getStatus()) || USED.equals(target.getStatus())) {
            throw new IllegalArgumentException("已占用或已使用号码不能指定为龙头");
        }
        if (chain.getCurrentHeadNumberId() != null && !chain.getCurrentHeadNumberId().equals(numberId)) {
            ReferralNumber oldHead = requiredNumberForUpdate(chain.getCurrentHeadNumberId());
            if (RESERVED.equals(oldHead.getStatus())) {
                throw new IllegalArgumentException("当前龙头已被订单占用，不能直接更换");
            }
            oldHead.setStatus(DISABLED);
            oldHead.setDisabledAt(LocalDateTime.now());
            oldHead.setOperatorName(principal.username());
            oldHead.setRemark("更换龙头：" + safeReason(reason));
            numberMapper.updateById(oldHead);
            recordHistory(oldHead, "DISABLE", null, null, principal, oldHead.getRemark());
        }
        target.setStatus(AVAILABLE);
        target.setDisabledAt(null);
        target.setOperatorName(principal.username());
        target.setRemark(reason);
        numberMapper.updateById(target);
        chain.setCurrentHeadNumberId(target.getId());
        chain.setOperatorName(principal.username());
        chainMapper.updateById(chain);
        recordHistory(target, "DESIGNATE_HEAD", null, null, principal, reason);
        logService.record(principal.username(), "REFERRAL_HEAD_DESIGNATE", "REFERRAL_CHAIN", chainId, null, target, reason);
        return target;
    }

    @Override
    @Transactional
    public ReferralNumber reserve(
            Long chainId,
            Long orderId,
            String reason,
            AdminPrincipal principal) {
        ReferralChain chain = requiredChainForUpdate(chainId);
        requireActive(chain);
        if (chain.getCurrentHeadNumberId() == null) {
            throw new IllegalArgumentException("当前接龙没有可用龙头");
        }
        ReferralNumber head = requiredNumberForUpdate(chain.getCurrentHeadNumberId());
        if (!AVAILABLE.equals(head.getStatus())) {
            throw new IllegalArgumentException("当前龙头已被占用，请等待该订单上台或释放");
        }
        MobilePlanOrder order = requiredOrder(orderId);
        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (!ReferralEligibility.isStudentOrder(order, customer)) {
            throw new IllegalArgumentException("只有学生订单可以分配推荐号码");
        }
        head.setStatus(RESERVED);
        head.setAssignedOrderId(orderId);
        head.setAssignedCustomerId(order.getCustomerId());
        head.setReservedAt(LocalDateTime.now());
        head.setOperatorName(principal.username());
        head.setRemark(reason);
        try {
            numberMapper.updateById(head);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("该订单已经分配了推荐号码");
        }
        order.setReferrerPhone(head.getReferralNumber());
        orderMapper.updateById(order);
        recordHistory(head, "RESERVE", order.getCustomerId(), orderId, principal, reason);
        logService.record(principal.username(), "REFERRAL_NUMBER_RESERVE", "REFERRAL_NUMBER", head.getId(), null, head, reason);
        return head;
    }

    @Override
    @Transactional
    public ReferralNumber release(Long numberId, String reason, AdminPrincipal principal) {
        String releaseReason = required(reason, "释放必须填写原因");
        ReferralNumber snapshot = requiredNumber(numberId);
        ReferralChain chain = requiredChainForUpdate(snapshot.getChainId());
        ReferralNumber number = requiredNumberForUpdate(numberId);
        if (!numberId.equals(chain.getCurrentHeadNumberId()) || !RESERVED.equals(number.getStatus())) {
            throw new IllegalArgumentException("只有当前已占用龙头可以释放");
        }
        Long orderId = number.getAssignedOrderId();
        Long customerId = number.getAssignedCustomerId();
        number.setStatus(AVAILABLE);
        number.setAssignedOrderId(null);
        number.setAssignedCustomerId(null);
        number.setReservedAt(null);
        number.setOperatorName(principal.username());
        number.setRemark(releaseReason);
        numberMapper.updateById(number);
        clearOrderReferrer(orderId, number.getReferralNumber());
        recordHistory(number, "RELEASE", customerId, orderId, principal, releaseReason);
        logService.record(principal.username(), "REFERRAL_NUMBER_RELEASE", "REFERRAL_NUMBER", numberId, null, number, releaseReason);
        return number;
    }

    @Override
    @Transactional
    public ReferralNumber completeOnboarding(
            Long numberId,
            String reason,
            AdminPrincipal principal) {
        ReferralNumber snapshot = requiredNumber(numberId);
        ReferralChain chain = requiredChainForUpdate(snapshot.getChainId());
        ReferralNumber oldHead = requiredNumberForUpdate(numberId);
        if (!numberId.equals(chain.getCurrentHeadNumberId()) || !RESERVED.equals(oldHead.getStatus())) {
            throw new IllegalArgumentException("只有当前已占用龙头可以完成接龙");
        }
        MobilePlanOrder order = requiredOrder(oldHead.getAssignedOrderId());
        if (!ONBOARDED_STATUSES.contains(order.getStatus())) {
            throw new IllegalArgumentException("订单尚未进入待激活、已激活或已完成状态");
        }
        String newNumberValue = normalizeNumber(order.getServiceNumber());
        ReferralNumber newHead = new ReferralNumber();
        newHead.setChainId(chain.getId());
        newHead.setReferralNumber(newNumberValue);
        newHead.setStatus(AVAILABLE);
        newHead.setSourceType("ORDER");
        newHead.setSourceOrderId(order.getId());
        newHead.setPreviousNumberId(oldHead.getId());
        newHead.setOperatorName(principal.username());
        newHead.setRemark("订单上台后成为新龙头");
        try {
            numberMapper.insert(newHead);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("订单上台号码已属于其他接龙");
        }
        oldHead.setStatus(USED);
        oldHead.setUsedAt(LocalDateTime.now());
        oldHead.setNextNumberId(newHead.getId());
        oldHead.setOperatorName(principal.username());
        oldHead.setRemark(reason);
        numberMapper.updateById(oldHead);
        chain.setCurrentHeadNumberId(newHead.getId());
        chain.setOperatorName(principal.username());
        chainMapper.updateById(chain);
        recordHistory(oldHead, "USE", oldHead.getAssignedCustomerId(), oldHead.getAssignedOrderId(), principal, reason);
        recordHistory(newHead, "BECOME_HEAD", order.getCustomerId(), order.getId(), principal, "上台号码成为新龙头");
        logService.record(principal.username(), "REFERRAL_CHAIN_ADVANCE", "REFERRAL_CHAIN", chain.getId(), oldHead, newHead, reason);
        return newHead;
    }

    @Override
    @Transactional
    public ReferralNumber disable(Long numberId, String reason, AdminPrincipal principal) {
        String disableReason = required(reason, "停用必须填写原因");
        ReferralNumber snapshot = requiredNumber(numberId);
        ReferralChain chain = requiredChainForUpdate(snapshot.getChainId());
        ReferralNumber number = requiredNumberForUpdate(numberId);
        if (RESERVED.equals(number.getStatus()) || USED.equals(number.getStatus())) {
            throw new IllegalArgumentException("已占用或已使用号码不能停用");
        }
        number.setStatus(DISABLED);
        number.setDisabledAt(LocalDateTime.now());
        number.setOperatorName(principal.username());
        number.setRemark(disableReason);
        numberMapper.updateById(number);
        if (numberId.equals(chain.getCurrentHeadNumberId())) {
            chain.setCurrentHeadNumberId(null);
            chainMapper.updateById(chain);
        }
        recordHistory(number, "DISABLE", null, null, principal, disableReason);
        logService.record(principal.username(), "REFERRAL_NUMBER_DISABLE", "REFERRAL_NUMBER", numberId, null, number, disableReason);
        return number;
    }

    @Override
    public List<ReferralNumberHistory> history(Long numberId) {
        return historyMapper.selectList(new LambdaQueryWrapper<ReferralNumberHistory>()
                .eq(ReferralNumberHistory::getReferralNumberId, numberId)
                .orderByDesc(ReferralNumberHistory::getId));
    }

    @Override
    public Map<String, Object> orderResources(Long orderId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iccids", iccidMapper.selectList(new LambdaQueryWrapper<IccidInventory>()
                .eq(IccidInventory::getCurrentOrderId, orderId)
                .orderByDesc(IccidInventory::getId)));
        result.put("referralNumbers", numberMapper.selectList(new LambdaQueryWrapper<ReferralNumber>()
                .and(query -> query.eq(ReferralNumber::getAssignedOrderId, orderId)
                        .or()
                        .eq(ReferralNumber::getSourceOrderId, orderId))
                .orderByDesc(ReferralNumber::getId)));
        return result;
    }

    @Override
    public Map<String, Object> diagnostics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availableRealIccids", iccidMapper.selectCount(new LambdaQueryWrapper<IccidInventory>()
                .eq(IccidInventory::getStatus, AVAILABLE)
                .eq(IccidInventory::getCardType, "REAL")));
        result.put("virtualPendingReplacement", iccidMapper.selectCount(new LambdaQueryWrapper<IccidInventory>()
                .eq(IccidInventory::getStatus, USED)
                .eq(IccidInventory::getCardType, "VIRTUAL")));
        result.put("activeChains", chainMapper.selectCount(new LambdaQueryWrapper<ReferralChain>()
                .eq(ReferralChain::getStatus, ACTIVE)));
        result.put("interruptedChains", chainMapper.selectCount(new LambdaQueryWrapper<ReferralChain>()
                .eq(ReferralChain::getStatus, ACTIVE)
                .isNull(ReferralChain::getCurrentHeadNumberId)));
        result.put("reservedHeads", numberMapper.selectCount(new LambdaQueryWrapper<ReferralNumber>()
                .eq(ReferralNumber::getStatus, RESERVED)));
        result.put("studentOrdersMissingReferral", eligibleOrders().size());
        List<Map<String, Object>> chainDetails = new ArrayList<>();
        for (ReferralChain chain : chainMapper.selectList(new LambdaQueryWrapper<ReferralChain>()
                .orderByDesc(ReferralChain::getId))) {
            ReferralNumber head = chain.getCurrentHeadNumberId() == null
                    ? null
                    : numberMapper.selectById(chain.getCurrentHeadNumberId());
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("chainId", chain.getId());
            detail.put("chainName", chain.getChainName());
            detail.put("chainStatus", chain.getStatus());
            detail.put("headNumber", head == null ? null : head.getReferralNumber());
            detail.put("headStatus", head == null ? null : head.getStatus());
            detail.put("interrupted", ACTIVE.equals(chain.getStatus()) && head == null);
            chainDetails.add(detail);
        }
        result.put("chains", chainDetails);
        return result;
    }

    private void clearOrderReferrer(Long orderId, String referralNumber) {
        if (orderId == null) {
            return;
        }
        orderMapper.update(null, new LambdaUpdateWrapper<MobilePlanOrder>()
                .eq(MobilePlanOrder::getId, orderId)
                .eq(MobilePlanOrder::getReferrerPhone, referralNumber)
                .set(MobilePlanOrder::getReferrerPhone, null));
    }

    private void recordHistory(
            ReferralNumber number,
            String action,
            Long customerId,
            Long orderId,
            AdminPrincipal principal,
            String reason) {
        ReferralNumberHistory history = new ReferralNumberHistory();
        history.setChainId(number.getChainId());
        history.setReferralNumberId(number.getId());
        history.setReferralNumber(number.getReferralNumber());
        history.setCustomerId(customerId);
        history.setOrderId(orderId);
        history.setActionType(action);
        history.setOperatorUserId(principal.userId());
        history.setOperatorName(principal.username());
        history.setReason(reason);
        historyMapper.insert(history);
    }

    private ReferralChain requiredChainForUpdate(Long id) {
        ReferralChain chain = chainMapper.selectByIdForUpdate(id);
        if (chain == null) {
            throw new IllegalArgumentException("接龙不存在");
        }
        return chain;
    }

    private ReferralNumber requiredNumberForUpdate(Long id) {
        ReferralNumber number = numberMapper.selectByIdForUpdate(id);
        if (number == null) {
            throw new IllegalArgumentException("推荐号码不存在");
        }
        return number;
    }

    private ReferralNumber requiredNumber(Long id) {
        ReferralNumber number = numberMapper.selectById(id);
        if (number == null) {
            throw new IllegalArgumentException("推荐号码不存在");
        }
        return number;
    }

    private MobilePlanOrder requiredOrder(Long id) {
        MobilePlanOrder order = id == null ? null : orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private void requireActive(ReferralChain chain) {
        if (!ACTIVE.equals(chain.getStatus())) {
            throw new IllegalArgumentException("只有启用中的接龙可以执行此操作");
        }
    }

    private String normalizeNumber(String value) {
        String normalized = required(value, "推荐号码不能为空").replace(" ", "");
        if (!normalized.matches("[0-9]{8,20}")) {
            throw new IllegalArgumentException("推荐号码应为8至20位数字");
        }
        return normalized;
    }

    private String validateImportNumber(String raw, Set<String> seen) {
        String number;
        try {
            number = normalizeNumber(raw);
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
        if (!seen.add(number)) {
            return "文件内号码重复";
        }
        if (numberMapper.selectCount(new LambdaQueryWrapper<ReferralNumber>()
                .eq(ReferralNumber::getReferralNumber, number)) > 0) {
            return "号码已存在于推荐号码池";
        }
        return null;
    }

    private String firstValue(Map<String, String> row) {
        for (String key : List.of("推荐号码", "上台号码", "referralNumber", "serviceNumber", "号码")) {
            if (hasText(row.get(key))) {
                return row.get(key);
            }
        }
        return row.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("__"))
                .map(Map.Entry::getValue)
                .filter(this::hasText)
                .findFirst()
                .orElse(null);
    }

    private String fileHash(MultipartFile file) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new IllegalArgumentException("无法读取导入文件", exception);
        }
    }

    private String required(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeReason(String reason) {
        return reason == null ? "" : reason;
    }

    private ReferralChain copyChain(ReferralChain source) {
        ReferralChain target = new ReferralChain();
        target.setId(source.getId());
        target.setChainCode(source.getChainCode());
        target.setChainName(source.getChainName());
        target.setStatus(source.getStatus());
        target.setCurrentHeadNumberId(source.getCurrentHeadNumberId());
        target.setOperatorName(source.getOperatorName());
        target.setRemark(source.getRemark());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
