package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.admin.entity.OperationLog;
import com.cmhk.business.module.admin.mapper.OperationLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {
    private final OperationLogMapper mapper;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void record(String operator, String operation, String objectType, Object objectId,
                       Object before, Object after, String remark) {
        OperationLog log = new OperationLog();
        log.setOperatorName(operator);
        log.setOperationType(operation);
        log.setObjectType(objectType);
        log.setObjectId(objectId == null ? null : String.valueOf(objectId));
        log.setBeforeData(toJson(before));
        log.setAfterData(toJson(after));
        log.setRemark(remark);
        mapper.insert(log);
    }

    public List<OperationLog> list(String objectType, String operationType) {
        return mapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(objectType != null && !objectType.isBlank(), OperationLog::getObjectType, objectType)
                .eq(operationType != null && !operationType.isBlank(), OperationLog::getOperationType, operationType)
                .orderByDesc(OperationLog::getId)
                .last("LIMIT 500"));
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { return String.valueOf(value); }
    }
}
