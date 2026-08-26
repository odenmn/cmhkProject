package com.cmhk.business.module.admin.service;

import com.cmhk.business.module.admin.entity.IccidAssignmentHistory;
import com.cmhk.business.module.admin.entity.IccidInventory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 管理端 ICCID 查询缓存与写操作门面。 */
public interface AdminIccidService {
    List<Map<String, Object>> list(String iccid, String batch, String status, String phone, String orderNo);

    IccidInventory create(String iccid, String batch, String remark, String operator);

    Map<String, Integer> importFile(MultipartFile file, String batch, String operator);

    IccidInventory assign(Long id, Long customerId, Long orderId, String reason, String operator);

    IccidInventory unassign(Long id, String reason, String operator);

    IccidInventory markUsed(Long id, String operator);

    IccidInventory disable(Long id, String reason, String operator);

    List<IccidAssignmentHistory> history(Long id);
}
