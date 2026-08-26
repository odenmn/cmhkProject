package com.cmhk.business.module.admin.service;

import com.cmhk.business.module.admin.dto.CustomerBackupSimulationPreview;
import org.springframework.web.multipart.MultipartFile;

/** CMHK 客户备份只读模拟服务。 */
public interface CustomerBackupSimulationService {

    /**
     * 解析客户备份并生成客户、订单和 ICCID 候选。
     *
     * @param file CMHK 客户备份 JSON
     * @return 不写数据库的模拟预览
     */
    CustomerBackupSimulationPreview simulate(MultipartFile file);
}
