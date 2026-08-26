package com.cmhk.business.module.admin.controller;

import com.cmhk.business.common.ApiResponse;
import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview;
import com.cmhk.business.module.admin.dto.CustomerBackupImportResult;
import com.cmhk.business.module.admin.service.CustomerBackupImportService;
import com.cmhk.business.module.admin.service.CustomerBackupSchemaService;
import com.cmhk.business.module.admin.service.CustomerBackupSimulationService;
import com.cmhk.business.config.AdminAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 管理端客户备份模拟导入接口。 */
@RestController
@RequestMapping("/api/admin/customer-backups")
public class AdminCustomerBackupController {

    private static final Logger log = LoggerFactory.getLogger(AdminCustomerBackupController.class);

    private final CustomerBackupSimulationService simulationService;
    private final CustomerBackupImportService importService;
    private final CustomerBackupSchemaService schemaService;

    public AdminCustomerBackupController(
            CustomerBackupSimulationService simulationService,
            CustomerBackupImportService importService,
            CustomerBackupSchemaService schemaService) {
        this.simulationService = simulationService;
        this.importService = importService;
        this.schemaService = schemaService;
    }

    /**
     * 只读解析客户备份并返回拟生成数据。
     *
     * <p>该接口不会调用数据库写操作，也不会记录客户敏感字段。</p>
     */
    @PostMapping(value = "/simulate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CustomerBackupSimulationPreview> simulate(@RequestParam("file") MultipartFile file) {
        log.info("开始模拟 CMHK 客户备份，文件大小={}", file.getSize());
        CustomerBackupSimulationPreview preview = simulationService.simulate(file);
        log.info(
                "CMHK 客户备份模拟完成，来源记录数={}，客户候选数={}，订单候选数={}，ICCID候选数={}，异常数={}",
                preview.summary().totalRecords(),
                preview.summary().customerCandidates(),
                preview.summary().orderCandidates(),
                preview.summary().totalIccidCandidates(),
                preview.summary().exceptionCount()
        );
        return ApiResponse.success(preview);
    }

    /** 根据模拟接口返回的文件摘要确认事务导入。 */
    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CustomerBackupImportResult> confirm(
            @RequestParam("file") MultipartFile file,
            @RequestParam("previewHash") String previewHash,
            HttpServletRequest request) {
        String operator = (String) request.getAttribute(AdminAuthInterceptor.ADMIN_USERNAME);
        schemaService.ensureReady();
        CustomerBackupImportResult result = importService.confirm(file, previewHash, operator);
        log.info(
                "CMHK 客户备份确认完成，批次={}，客户={}，订单={}，ICCID={}，异常={}",
                result.importId(),
                result.customersCreated() + result.customersReused(),
                result.ordersCreated() + result.ordersReused(),
                result.iccidsCreated() + result.iccidsReused(),
                result.exceptionCount()
        );
        return ApiResponse.success(result);
    }

    /** 将已导入客户的业务类别移出需求摘要，并统一为数字状态码。 */
    @PostMapping("/customer-model/migrate")
    public ApiResponse<Map<String, Integer>> migrateCustomerModel() {
        Map<String, Integer> result = schemaService.normalizeCustomerModel();
        log.info(
                "客户模型迁移完成，备份客户={}，已分类={}，占用需求摘要={}，已上台={}",
                result.get("backupCustomers"),
                result.get("categorizedCustomers"),
                result.get("occupiedRequirementSummaries"),
                result.get("onboardedCustomers")
        );
        return ApiResponse.success(result);
    }
}
