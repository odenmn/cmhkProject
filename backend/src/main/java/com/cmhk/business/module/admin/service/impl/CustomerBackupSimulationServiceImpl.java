package com.cmhk.business.module.admin.service.impl;

import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview.CustomerCandidate;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview.ExceptionCandidate;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview.IccidCandidate;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview.OrderCandidate;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview.Summary;
import com.cmhk.business.module.admin.service.CustomerBackupSimulationService;
import com.cmhk.business.module.customer.entity.CustomerStatusCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** CMHK 客户备份只读模拟服务实现。 */
@Service
public class CustomerBackupSimulationServiceImpl implements CustomerBackupSimulationService {

    private static final String ORDER_SOURCE = "CMHK_BACKUP";
    private static final String REAL = "REAL";
    private static final String VIRTUAL = "VIRTUAL";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String USED = "USED";
    private static final BigInteger VIRTUAL_NUMBER_MODULUS = BigInteger.TEN.pow(18);

    private final ObjectMapper objectMapper;

    public CustomerBackupSimulationServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 只在内存中构造候选数据，不调用 Mapper 或数据库写接口。 */
    @Override
    public CustomerBackupSimulationPreview simulate(MultipartFile file) {
        List<SourceRow> sourceRows = readSourceRows(file);
        Map<String, Integer> sourceIdCounts = countSourceIds(sourceRows);
        Map<String, Integer> validIccidCounts = countValidIccids(sourceRows);
        Set<String> reservedIccids = collectUniqueRealIccids(validIccidCounts);

        List<CustomerCandidate> customers = new ArrayList<>();
        List<OrderCandidate> orders = new ArrayList<>();
        List<IccidCandidate> iccids = new ArrayList<>();
        List<ExceptionCandidate> exceptions = new ArrayList<>();
        int onboardedRecords = 0;
        int validRealIccidRows = 0;

        for (SourceRow row : sourceRows) {
            if (isValidRealIccid(row.iccid())) {
                validRealIccidRows++;
            }
            if (!isUsableSourceId(row, sourceIdCounts, exceptions)) {
                continue;
            }

            String sourceCustomerKey = ORDER_SOURCE + ":" + row.sourceId();
            String orderNo = stableOrderNo(row.sourceId());
            int customerStatus = CustomerStatusCode.fromBackup(row.stage(), row.hasOnboardDate());
            boolean onboarded = CustomerStatusCode.isOnboarded(customerStatus);
            if (onboarded) {
                onboardedRecords++;
            }
            customers.add(toCustomer(row, sourceCustomerKey, customerStatus));
            if (onboarded) {
                orders.add(toOrder(row, sourceCustomerKey, orderNo));
            }
            addIccidCandidate(row, sourceCustomerKey, onboarded ? orderNo : null, onboarded, validIccidCounts,
                    reservedIccids, iccids, exceptions);
        }

        int realIccidCandidates = (int) iccids.stream()
                .filter(candidate -> REAL.equals(candidate.cardType()))
                .count();
        int virtualIccidCandidates = (int) iccids.stream()
                .filter(candidate -> VIRTUAL.equals(candidate.cardType()))
                .count();
        int boundIccidCandidates = (int) iccids.stream()
                .filter(IccidCandidate::bound)
                .count();

        Summary summary = new Summary(
                sourceRows.size(),
                customers.size(),
                orders.size(),
                onboardedRecords,
                validRealIccidRows,
                realIccidCandidates,
                virtualIccidCandidates,
                iccids.size(),
                boundIccidCandidates,
                exceptions.size()
        );
        List<String> warnings = List.of(
                "源文件没有结构化客户手机号，模拟阶段使用来源系统和来源客户 ID 识别客户。",
                "源文件的 umall 字段是状态而非 UMALL 订单号，模拟阶段不会生成虚假 UMALL 订单号。",
                "本次结果仅为内存预览，未写入 customer、mobile_plan_order 或 iccid_inventory。"
        );
        return new CustomerBackupSimulationPreview(
                fileHash(file),
                summary,
                warnings,
                List.copyOf(customers),
                List.copyOf(orders),
                List.copyOf(iccids),
                List.copyOf(exceptions)
        );
    }

    private String fileHash(MultipartFile file) {
        try {
            return java.util.HexFormat.of().formatHex(sha256(file.getBytes()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("客户备份 JSON 摘要计算失败", exception);
        }
    }

    private byte[] sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }

    private List<SourceRow> readSourceRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("客户备份 JSON 不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            if (root == null || !root.isArray()) {
                throw new IllegalArgumentException("客户备份 JSON 根结构必须是数组");
            }
            List<SourceRow> rows = new ArrayList<>();
            int rowNumber = 1;
            for (JsonNode node : root) {
                rows.add(new SourceRow(
                        rowNumber,
                        text(node, "id"),
                        text(node, "name"),
                        text(node, "channel"),
                        text(node, "customerType"),
                        text(node, "plan"),
                        text(node, "onboardPlan"),
                        text(node, "stage"),
                        text(node, "umall"),
                        text(node, "number"),
                        text(node, "onboardDate"),
                        text(node, "iccid")
                ));
                rowNumber++;
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("客户备份 JSON 读取失败", exception);
        }
    }

    private Map<String, Integer> countSourceIds(List<SourceRow> rows) {
        Map<String, Integer> counts = new HashMap<>();
        for (SourceRow row : rows) {
            if (!row.sourceId().isBlank()) {
                counts.merge(row.sourceId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private Map<String, Integer> countValidIccids(List<SourceRow> rows) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SourceRow row : rows) {
            if (isValidRealIccid(row.iccid())) {
                counts.merge(row.iccid(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private Set<String> collectUniqueRealIccids(Map<String, Integer> counts) {
        Set<String> values = new HashSet<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) {
                values.add(entry.getKey());
            }
        }
        return values;
    }

    private boolean isUsableSourceId(
            SourceRow row,
            Map<String, Integer> sourceIdCounts,
            List<ExceptionCandidate> exceptions) {
        if (row.sourceId().isBlank()) {
            exceptions.add(exception(row, "SOURCE_ID_MISSING", "来源客户 ID 为空，无法生成稳定客户和订单", null));
            return false;
        }
        if (sourceIdCounts.getOrDefault(row.sourceId(), 0) > 1) {
            exceptions.add(exception(row, "SOURCE_ID_DUPLICATE", "来源客户 ID 重复，无法唯一生成客户和订单",
                    maskIdentifier(row.sourceId())));
            return false;
        }
        return true;
    }

    private CustomerCandidate toCustomer(SourceRow row, String sourceCustomerKey, int customerStatus) {
        return new CustomerCandidate(
                row.rowNumber(),
                row.sourceId(),
                sourceCustomerKey,
                row.name(),
                row.channel(),
                row.channel().isBlank() ? "DIRECT" : "CHANNEL",
                row.customerType(),
                preferredPlan(row),
                customerStatus
        );
    }

    private OrderCandidate toOrder(SourceRow row, String sourceCustomerKey, String orderNo) {
        return new OrderCandidate(
                row.rowNumber(),
                row.sourceId(),
                orderNo,
                sourceCustomerKey,
                row.serviceNumber(),
                preferredPlan(row),
                defaultText(row.stage(), "待处理"),
                row.umallStatus(),
                row.onboardDate(),
                true,
                ORDER_SOURCE
        );
    }

    private void addIccidCandidate(
            SourceRow row,
            String sourceCustomerKey,
            String orderNo,
            boolean onboarded,
            Map<String, Integer> validIccidCounts,
            Set<String> reservedIccids,
            List<IccidCandidate> iccids,
            List<ExceptionCandidate> exceptions) {
        if (row.iccid().isBlank()) {
            addVirtualIccid(row, sourceCustomerKey, orderNo, onboarded, reservedIccids, iccids, exceptions);
            return;
        }
        if (!isValidRealIccid(row.iccid())) {
            exceptions.add(exception(row, "ICCID_INVALID", "ICCID 非空但不是标准 20 位数字",
                    maskIdentifier(row.iccid())));
            return;
        }
        if (validIccidCounts.getOrDefault(row.iccid(), 0) > 1) {
            exceptions.add(exception(row, "ICCID_DUPLICATE", "ICCID 在来源文件中重复，禁止自动绑定",
                    maskIdentifier(row.iccid())));
            return;
        }

        boolean bound = onboarded && !row.serviceNumber().isBlank();
        if (onboarded && row.serviceNumber().isBlank()) {
            exceptions.add(exception(row, "SERVICE_NUMBER_MISSING", "已上台记录缺少上台号码，ICCID 仅进入可用卡池", null));
        }
        iccids.add(new IccidCandidate(
                row.rowNumber(),
                row.sourceId(),
                row.iccid(),
                REAL,
                bound ? USED : AVAILABLE,
                bound ? sourceCustomerKey : null,
                bound ? orderNo : null,
                bound ? row.serviceNumber() : null,
                bound
        ));
    }

    private void addVirtualIccid(
            SourceRow row,
            String sourceCustomerKey,
            String orderNo,
            boolean onboarded,
            Set<String> reservedIccids,
            List<IccidCandidate> iccids,
            List<ExceptionCandidate> exceptions) {
        if (!onboarded) {
            return;
        }
        if (row.serviceNumber().isBlank()) {
            exceptions.add(exception(row, "VIRTUAL_ICCID_SOURCE_MISSING",
                    "已上台但缺少上台号码，无法生成稳定虚拟 ICCID", null));
            return;
        }

        String virtualIccid = stableVirtualIccid(row.serviceNumber(), row.onboardDate());
        if (!reservedIccids.add(virtualIccid)) {
            exceptions.add(exception(row, "VIRTUAL_ICCID_COLLISION",
                    "稳定虚拟 ICCID 与其他卡号碰撞，禁止自动生成", maskIdentifier(virtualIccid)));
            return;
        }
        iccids.add(new IccidCandidate(
                row.rowNumber(),
                row.sourceId(),
                virtualIccid,
                VIRTUAL,
                USED,
                sourceCustomerKey,
                orderNo,
                row.serviceNumber(),
                true
        ));
    }

    private boolean isValidRealIccid(String value) {
        return value != null && value.matches("[0-9]{20}");
    }

    private String preferredPlan(SourceRow row) {
        if (!row.onboardPlan().isBlank()) {
            return row.onboardPlan();
        }
        return defaultText(row.intendedPlan(), "待确认套餐");
    }

    private String stableOrderNo(String sourceId) {
        if (sourceId.matches("[A-Za-z0-9_-]{1,40}")) {
            return "CMHK-BACKUP-" + sourceId;
        }
        return "CMHK-BACKUP-" + digestHex("ORDER|" + sourceId, 20);
    }

    private String stableVirtualIccid(String serviceNumber, String onboardDate) {
        byte[] digest = sha256("VIRTUAL_ICCID|" + serviceNumber + "|" + onboardDate);
        BigInteger numericDigest = new BigInteger(1, Arrays.copyOf(digest, 16));
        BigInteger suffix = numericDigest.mod(VIRTUAL_NUMBER_MODULUS);
        return "99" + String.format(Locale.ROOT, "%018d", suffix);
    }

    private String digestHex(String value, int length) {
        byte[] digest = sha256(value);
        StringBuilder hex = new StringBuilder();
        for (byte item : digest) {
            hex.append(String.format(Locale.ROOT, "%02x", item));
        }
        return hex.substring(0, length).toUpperCase(Locale.ROOT);
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }

    private ExceptionCandidate exception(
            SourceRow row,
            String code,
            String message,
            String maskedValue) {
        return new ExceptionCandidate(row.rowNumber(), row.sourceId(), code, message, maskedValue);
    }

    private String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2)
                + "(长度" + value.length() + ")";
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private String defaultText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private record SourceRow(
            int rowNumber,
            String sourceId,
            String name,
            String channel,
            String customerType,
            String intendedPlan,
            String onboardPlan,
            String stage,
            String umallStatus,
            String serviceNumber,
            String onboardDate,
            String iccid
    ) {

        /** 上台日期仅用于映射来源状态，不直接作为是否创建订单的判断条件。 */
        private boolean hasOnboardDate() {
            return onboardDate != null && !onboardDate.isBlank();
        }
    }
}
