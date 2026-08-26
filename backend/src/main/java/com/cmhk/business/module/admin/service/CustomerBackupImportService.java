package com.cmhk.business.module.admin.service;

import com.cmhk.business.module.admin.dto.CustomerBackupImportResult;
import org.springframework.web.multipart.MultipartFile;

/** 客户备份确认导入服务。 */
public interface CustomerBackupImportService {

    /** 校验预览摘要后，以单个事务确认写入客户、订单、卡池与异常明细。 */
    CustomerBackupImportResult confirm(MultipartFile file, String previewHash, String operator);
}
