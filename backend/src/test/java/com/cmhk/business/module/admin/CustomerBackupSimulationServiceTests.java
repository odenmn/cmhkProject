package com.cmhk.business.module.admin;

import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview;
import com.cmhk.business.module.admin.service.impl.CustomerBackupSimulationServiceImpl;
import com.cmhk.business.module.customer.entity.CustomerStatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 客户备份只读模拟规则测试。 */
class CustomerBackupSimulationServiceTests {

    private final CustomerBackupSimulationServiceImpl service =
            new CustomerBackupSimulationServiceImpl(new ObjectMapper());

    /** 验证客户、订单、真实卡、虚拟卡和异常数据能被正确分流。 */
    @Test
    void shouldBuildCandidatesWithoutWritingDatabase() {
        String json = """
                [
                  {"id":"SRC001","name":"客户一","channel":"渠道甲","stage":"已激活","umall":"已激活","number":"51234567","onboardDate":"2026-08-01","iccid":""},
                  {"id":"SRC002","name":"客户二","channel":"渠道甲","stage":"已激活","umall":"已激活","number":"52345678","onboardDate":"2026-08-02","iccid":"89860000000000000001"},
                  {"id":"SRC003","name":"客户三","channel":"渠道乙","stage":"已进群","umall":"未提交","number":"","onboardDate":"","iccid":""},
                  {"id":"SRC004","name":"客户四","channel":"渠道乙","stage":"待激活","umall":"待激活","number":"53456789","onboardDate":"2026-08-03","iccid":"89860000000000000002"},
                  {"id":"SRC005","name":"客户五","channel":"渠道乙","stage":"待激活","umall":"待激活","number":"54567890","onboardDate":"2026-08-04","iccid":"89860000000000000002"},
                  {"id":"SRC006","name":"客户六","channel":"渠道乙","stage":"待激活","umall":"待激活","number":"55678901","onboardDate":"2026-08-05","iccid":"无需"}
                ]
                """;
        CustomerBackupSimulationPreview preview = simulate(json);

        assertEquals(6, preview.summary().totalRecords());
        assertEquals(6, preview.summary().customerCandidates());
        assertEquals(6, preview.summary().orderCandidates());
        assertEquals(5, preview.summary().onboardedRecords());
        assertEquals(3, preview.summary().validRealIccidRows());
        assertEquals(1, preview.summary().realIccidCandidates());
        assertEquals(1, preview.summary().virtualIccidCandidates());
        assertEquals(2, preview.summary().totalIccidCandidates());
        assertEquals(2, preview.summary().boundIccidCandidates());
        assertEquals(3, preview.summary().exceptionCount());
        assertEquals(CustomerStatusCode.ACTIVATED, preview.customers().get(0).currentStatus());
        assertEquals(CustomerStatusCode.FOLLOWING, preview.customers().get(2).currentStatus());
        assertEquals(CustomerStatusCode.WAITING_ACTIVATION, preview.customers().get(3).currentStatus());

        CustomerBackupSimulationPreview.IccidCandidate virtualCard = preview.iccids().stream()
                .filter(candidate -> "VIRTUAL".equals(candidate.cardType()))
                .findFirst()
                .orElseThrow();
        assertTrue(virtualCard.iccid().matches("99[0-9]{18}"));
        assertEquals("51234567", virtualCard.serviceNumber());
        assertTrue(virtualCard.bound());
        assertEquals("USED", virtualCard.status());
    }

    /** 验证无上台日期时保留待激活与已激活状态，有上台日期时优先记为已激活。 */
    @Test
    void shouldMapNumericCustomerStatuses() {
        assertEquals(CustomerStatusCode.WAITING_ACTIVATION,
                CustomerStatusCode.fromBackup("待激活", false));
        assertEquals(CustomerStatusCode.ACTIVATED,
                CustomerStatusCode.fromBackup("已激活", false));
        assertEquals(CustomerStatusCode.WAITING_ACTIVATION,
                CustomerStatusCode.fromBackup("待激活", true));
        assertEquals(CustomerStatusCode.COMPLETED,
                CustomerStatusCode.fromBackup("已完成", false));
    }

    /** 相同上台号码和上台日期必须稳定生成相同虚拟 ICCID。 */
    @Test
    void shouldGenerateStableVirtualIccid() {
        String firstJson = """
                [{"id":"A001","number":"51234567","onboardDate":"2026-08-01","iccid":""}]
                """;
        String secondJson = """
                [{"id":"B001","number":"51234567","onboardDate":"2026-08-01","iccid":""}]
                """;
        String differentJson = """
                [{"id":"C001","number":"51234567","onboardDate":"2026-08-02","iccid":""}]
                """;

        String firstIccid = simulate(firstJson).iccids().getFirst().iccid();
        String secondIccid = simulate(secondJson).iccids().getFirst().iccid();
        String differentIccid = simulate(differentJson).iccids().getFirst().iccid();

        assertEquals(firstIccid, secondIccid);
        assertNotEquals(firstIccid, differentIccid);
    }

    /** 配置真实备份路径时，执行脱敏的数量级回归验证。 */
    @Test
    void shouldSimulateConfiguredRealBackup() throws IOException {
        String configuredPath = System.getProperty("cmhk.backup.file");
        assumeTrue(configuredPath != null && !configuredPath.isBlank());
        Path path = Path.of(configuredPath);
        assumeTrue(Files.isRegularFile(path));

        byte[] content = Files.readAllBytes(path);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                path.getFileName().toString(),
                "application/json",
                content
        );
        CustomerBackupSimulationPreview preview = service.simulate(file);

        assertEquals(233, preview.summary().totalRecords());
        assertEquals(233, preview.summary().customerCandidates());
        assertEquals(233, preview.summary().orderCandidates());
        assertEquals(132, preview.summary().onboardedRecords());
        assertEquals(100, preview.summary().validRealIccidRows());
        assertEquals(98, preview.summary().realIccidCandidates());
        assertEquals(33, preview.summary().virtualIccidCandidates());
        assertEquals(131, preview.summary().totalIccidCandidates());
        assertEquals(127, preview.summary().boundIccidCandidates());
        assertEquals(6, preview.summary().exceptionCount());
    }

    private CustomerBackupSimulationPreview simulate(String json) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "backup.json",
                "application/json",
                json.getBytes()
        );
        return service.simulate(file);
    }
}
