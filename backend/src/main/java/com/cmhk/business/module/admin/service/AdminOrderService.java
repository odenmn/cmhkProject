package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter; import java.util.List; import java.util.concurrent.ThreadLocalRandom;

@Service
public class AdminOrderService {
    private final MobilePlanOrderMapper mapper; private final CustomerMapper customerMapper; private final OperationLogService logService;
    public AdminOrderService(MobilePlanOrderMapper mapper, CustomerMapper customerMapper, OperationLogService logService) { this.mapper=mapper; this.customerMapper=customerMapper; this.logService=logService; }

    public List<MobilePlanOrder> list(String keyword, String status, Long customerId) {
        return mapper.selectList(new LambdaQueryWrapper<MobilePlanOrder>()
                .and(keyword!=null&&!keyword.isBlank(), q->q.like(MobilePlanOrder::getOrderNo,keyword).or().like(MobilePlanOrder::getUmallOrderNo,keyword).or().like(MobilePlanOrder::getContactPhone,keyword).or().like(MobilePlanOrder::getServiceNumber,keyword))
                .eq(status!=null&&!status.isBlank(), MobilePlanOrder::getStatus,status)
                .eq(customerId!=null, MobilePlanOrder::getCustomerId,customerId).orderByDesc(MobilePlanOrder::getId));
    }

    @Transactional public MobilePlanOrder save(Long id, MobilePlanOrder input, String operator) {
        MobilePlanOrder before=id==null?null:required(id); MobilePlanOrder target=new MobilePlanOrder(); if(before!=null)BeanUtils.copyProperties(before,target);
        if (input.getCustomerId()==null || customerMapper.selectById(input.getCustomerId())==null) throw new IllegalArgumentException("请选择有效客户");
        target.setCustomerId(input.getCustomerId()); target.setCustomerName(input.getCustomerName()); target.setContactPhone(required(input.getContactPhone(),"联系电话不能为空"));
        target.setPlanId(input.getPlanId()); target.setPlanCode(required(input.getPlanCode(),"套餐编码不能为空")); target.setPlanName(required(input.getPlanName(),"套餐名称不能为空"));
        target.setPlanType(input.getPlanType()); target.setMonthlyFee(input.getMonthlyFee()==null? BigDecimal.ZERO:input.getMonthlyFee()); target.setContractPeriod(input.getContractPeriod());
        target.setUmallOrderNo(input.getUmallOrderNo()); target.setServiceNumber(input.getServiceNumber()); target.setActivationStatus(input.getActivationStatus()); target.setContractStatus(input.getContractStatus());
        target.setOrderSource(input.getOrderSource()==null?"ADMIN":input.getOrderSource()); target.setStatus(input.getStatus()==null?"待处理":input.getStatus());
        target.setReconciliationStatus(input.getReconciliationStatus()==null?"待对账":input.getReconciliationStatus());
        if(id==null){ target.setOrderNo("ADM"+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+ThreadLocalRandom.current().nextInt(100,1000)); mapper.insert(target); }
        else mapper.updateById(target);
        logService.record(operator,id==null?"ORDER_CREATE":"ORDER_UPDATE","ORDER",target.getId(),before,target,null); return target;
    }
    public MobilePlanOrder required(Long id){MobilePlanOrder o=mapper.selectById(id);if(o==null)throw new IllegalArgumentException("订单不存在");return o;}
    private String required(String v,String m){if(v==null||v.isBlank())throw new IllegalArgumentException(m);return v.trim();}
}
