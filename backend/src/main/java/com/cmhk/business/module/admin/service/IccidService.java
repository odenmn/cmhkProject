package com.cmhk.business.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cmhk.business.module.admin.entity.IccidAssignmentHistory;
import com.cmhk.business.module.admin.entity.IccidInventory;
import com.cmhk.business.module.admin.mapper.IccidAssignmentHistoryMapper;
import com.cmhk.business.module.admin.mapper.IccidInventoryMapper;
import com.cmhk.business.module.customer.entity.Customer;
import com.cmhk.business.module.customer.mapper.CustomerMapper;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import com.cmhk.business.module.mobile.mapper.MobilePlanOrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IccidService {
    public static final String AVAILABLE="AVAILABLE", ASSIGNED="ASSIGNED", USED="USED", DISABLED="DISABLED", REPLACED="REPLACED";
    private final IccidInventoryMapper mapper; private final IccidAssignmentHistoryMapper historyMapper; private final CustomerMapper customerMapper; private final MobilePlanOrderMapper orderMapper; private final OperationLogService logService; private final TabularFileReader fileReader;
    public IccidService(IccidInventoryMapper mapper,IccidAssignmentHistoryMapper historyMapper,CustomerMapper customerMapper,MobilePlanOrderMapper orderMapper,OperationLogService logService,TabularFileReader fileReader){this.mapper=mapper;this.historyMapper=historyMapper;this.customerMapper=customerMapper;this.orderMapper=orderMapper;this.logService=logService;this.fileReader=fileReader;}

    public List<Map<String,Object>> list(String iccid,String batch,String status,String phone,String orderNo){
        List<IccidInventory> rows=mapper.selectList(new LambdaQueryWrapper<IccidInventory>().like(notBlank(iccid),IccidInventory::getIccid,iccid).eq(notBlank(batch),IccidInventory::getBatchNo,batch).eq(notBlank(status),IccidInventory::getStatus,status).orderByDesc(IccidInventory::getId));
        List<Map<String,Object>> result=new ArrayList<>(); for(IccidInventory row:rows){Customer c=row.getCurrentCustomerId()==null?null:customerMapper.selectById(row.getCurrentCustomerId());MobilePlanOrder o=row.getCurrentOrderId()==null?null:orderMapper.selectById(row.getCurrentOrderId());
            if(notBlank(phone)&&(c==null||c.getPhone()==null||!c.getPhone().contains(phone)))continue;if(notBlank(orderNo)&&(o==null||(!contains(o.getOrderNo(),orderNo)&&!contains(o.getUmallOrderNo(),orderNo))))continue;
            Map<String,Object> item=new LinkedHashMap<>();item.put("inventory",row);item.put("customerName",c==null?null:c.getName());item.put("customerPhone",c==null?null:c.getPhone());item.put("orderNo",o==null?null:o.getOrderNo());item.put("umallOrderNo",o==null?null:o.getUmallOrderNo());result.add(item);}return result;
    }

    @Transactional
    public IccidInventory create(String iccid,String batch,String remark,String operator){
        String normalized=normalize(iccid);
        if(mapper.selectCount(new LambdaQueryWrapper<IccidInventory>().eq(IccidInventory::getIccid,normalized))>0)throw new IllegalArgumentException("ICCID已存在");
        IccidInventory row=new IccidInventory();
        row.setIccid(normalized);
        row.setBatchNo(batch);
        row.setStatus(AVAILABLE);
        row.setOperatorName(operator);row.setRemark(remark);
        mapper.insert(row);
        logService.record(operator,"ICCID_CREATE","ICCID",row.getId(),null,row,null);return row;}

    @Transactional
    public Map<String,Integer> importFile(MultipartFile file,String batch,String operator){
        List<Map<String,String>> source=fileReader.read(file);
        int success=0,duplicate=0,failed=0;
        for(Map<String,String> r:source){
            String value=first(r,"ICCID","iccid","卡号","SIM卡号");
            if(value==null&& !r.isEmpty()) value=r.entrySet().stream().filter(e->!e.getKey().startsWith("__")).map(Map.Entry::getValue).findFirst().orElse(null);
            try{
                create(value,batch,null,operator);success++;
            }catch(DuplicateKeyException|IllegalArgumentException ex) {
                if(ex.getMessage()!=null&&ex.getMessage().contains("存在"))duplicate++;
                else failed++;}
        }
        logService.record(operator,"ICCID_IMPORT","ICCID_BATCH",batch,null,Map.of("total",source.size(),"success",success,"duplicate",duplicate,"failed",failed),file.getOriginalFilename());return Map.of("total",source.size(),"success",success,"duplicate",duplicate,"failed",failed);
    }

    @Transactional
    public IccidInventory assign(Long id,Long customerId,Long orderId,String reason,String operator){
        IccidInventory before=required(id);
        Customer customer=customerMapper.selectById(customerId);
        MobilePlanOrder order=orderMapper.selectById(orderId);
        if(customer==null||order==null)throw new IllegalArgumentException("客户或订单不存在");
        if(!Objects.equals(order.getCustomerId(),customerId))throw new IllegalArgumentException("订单不属于所选客户");
        int changed=mapper.update(null,new LambdaUpdateWrapper<IccidInventory>().eq(IccidInventory::getId,id).eq(IccidInventory::getStatus,AVAILABLE).set(IccidInventory::getStatus,ASSIGNED).set(IccidInventory::getCurrentCustomerId,customerId).set(IccidInventory::getCurrentOrderId,orderId).set(IccidInventory::getAssignedAt,LocalDateTime.now()).set(IccidInventory::getOperatorName,operator));
        if(changed!=1)throw new IllegalArgumentException("ICCID不是可用状态，可能已被其他人分配");
        IccidInventory after=required(id);history(after,"ASSIGN",customerId,orderId,reason,operator);
        logService.record(operator,"ICCID_ASSIGN","ICCID",id,before,after,reason);return after;
    }

    @Transactional
    public IccidInventory unassign(Long id,String reason,String operator){
        if(!notBlank(reason))throw new IllegalArgumentException("解绑必须填写原因");
        IccidInventory before=required(id);
        if(!ASSIGNED.equals(before.getStatus()))throw new IllegalArgumentException("只有已占用ICCID可以解绑");
        int changed=mapper.update(null,new LambdaUpdateWrapper<IccidInventory>()
                .eq(IccidInventory::getId,id)
                .eq(IccidInventory::getStatus,ASSIGNED)
                .set(IccidInventory::getStatus,AVAILABLE)
                .set(IccidInventory::getCurrentCustomerId,null)
                .set(IccidInventory::getCurrentOrderId,null)
                .set(IccidInventory::getAssignedAt,null)
                .set(IccidInventory::getOperatorName,operator));
        if(changed!=1)throw new IllegalArgumentException("ICCID状态已变化，请刷新后重试");
        history(before,"UNASSIGN",before.getCurrentCustomerId(),before.getCurrentOrderId(),reason,operator);
        IccidInventory after=required(id);
        logService.record(operator,"ICCID_UNASSIGN","ICCID",id,before,after,reason);
        return after;
    }

    @Transactional
    public IccidInventory markUsed(Long id,String operator){
        IccidInventory before=required(id);
        if(!ASSIGNED.equals(before.getStatus()))throw new IllegalArgumentException("只有已占用ICCID可以标记使用");
        int changed=mapper.update(null,new LambdaUpdateWrapper<IccidInventory>()
                .eq(IccidInventory::getId,id)
                .eq(IccidInventory::getStatus,ASSIGNED)
                .set(IccidInventory::getStatus,USED)
                .set(IccidInventory::getUsedAt,LocalDateTime.now())
                .set(IccidInventory::getOperatorName,operator));
        if(changed!=1)throw new IllegalArgumentException("ICCID状态已变化，请刷新后重试");
        IccidInventory after=required(id);
        history(after,"MARK_USED",after.getCurrentCustomerId(),after.getCurrentOrderId(),null,operator);
        logService.record(operator,"ICCID_MARK_USED","ICCID",id,before,after,null);return after;}

    @Transactional
    public IccidInventory disable(Long id,String reason,String operator){
        if(!notBlank(reason))throw new IllegalArgumentException("停用必须填写原因");
        IccidInventory before=required(id);
        if(!AVAILABLE.equals(before.getStatus()))throw new IllegalArgumentException("只有可用ICCID可以停用");
        int changed=mapper.update(null,new LambdaUpdateWrapper<IccidInventory>()
                .eq(IccidInventory::getId,id)
                .eq(IccidInventory::getStatus,AVAILABLE)
                .set(IccidInventory::getStatus,DISABLED)
                .set(IccidInventory::getRemark,reason)
                .set(IccidInventory::getOperatorName,operator));
        if(changed!=1)throw new IllegalArgumentException("ICCID状态已变化，请刷新后重试");
        IccidInventory after=required(id);
        logService.record(operator,"ICCID_DISABLE","ICCID",id,before,after,reason);
        return after;
    }

    /** 将虚拟卡的当前关系原子迁移到一张真实可用卡。 */
    @Transactional
    public IccidInventory replaceVirtual(Long virtualId,Long realId,String reason,String operator){
        if(!notBlank(reason))throw new IllegalArgumentException("替换必须填写原因");
        if(Objects.equals(virtualId,realId))throw new IllegalArgumentException("虚拟卡和真实卡不能相同");
        Long firstId=Math.min(virtualId,realId);
        Long secondId=Math.max(virtualId,realId);
        IccidInventory first=mapper.selectByIdForUpdate(firstId);
        IccidInventory second=mapper.selectByIdForUpdate(secondId);
        if(first==null||second==null)throw new IllegalArgumentException("ICCID不存在");
        IccidInventory virtual=Objects.equals(first.getId(),virtualId)?first:second;
        IccidInventory real=Objects.equals(first.getId(),realId)?first:second;
        if(!"VIRTUAL".equals(virtual.getCardType())||!USED.equals(virtual.getStatus()))throw new IllegalArgumentException("只有使用中的虚拟ICCID可以替换");
        if(!"REAL".equals(real.getCardType())||!AVAILABLE.equals(real.getStatus()))throw new IllegalArgumentException("替换目标必须是真实可用ICCID");
        IccidInventory virtualBefore=copy(virtual);
        IccidInventory realBefore=copy(real);
        real.setStatus(USED);
        real.setCurrentCustomerId(virtual.getCurrentCustomerId());
        real.setCurrentOrderId(virtual.getCurrentOrderId());
        real.setServiceNumber(virtual.getServiceNumber());
        real.setAssignedAt(LocalDateTime.now());
        real.setUsedAt(LocalDateTime.now());
        real.setOperatorName(operator);
        real.setRemark(reason);
        mapper.updateById(real);
        history(real,"REPLACE_IN",real.getCurrentCustomerId(),real.getCurrentOrderId(),reason,operator);
        history(virtual,"REPLACE_OUT",virtual.getCurrentCustomerId(),virtual.getCurrentOrderId(),reason,operator);
        virtual.setStatus(REPLACED);
        virtual.setReplacedByIccidId(real.getId());
        virtual.setReplacedAt(LocalDateTime.now());
        virtual.setCurrentCustomerId(null);
        virtual.setCurrentOrderId(null);
        virtual.setAssignedAt(null);
        virtual.setUsedAt(null);
        virtual.setOperatorName(operator);
        virtual.setRemark(reason);
        mapper.updateById(virtual);
        logService.record(operator,"ICCID_REPLACE","ICCID",virtualId,Map.of("virtual",virtualBefore,"real",realBefore),Map.of("virtual",virtual,"real",real),reason);
        return real;
    }

    public List<IccidAssignmentHistory> history(Long id){return historyMapper.selectList(new LambdaQueryWrapper<IccidAssignmentHistory>().eq(IccidAssignmentHistory::getIccidId,id).orderByDesc(IccidAssignmentHistory::getId));}

    public long count(String status){return mapper.selectCount(new LambdaQueryWrapper<IccidInventory>().eq(IccidInventory::getStatus,status));}

    private IccidInventory required(Long id){IccidInventory row=mapper.selectById(id);if(row==null)throw new IllegalArgumentException("ICCID不存在");return row;}

    private IccidInventory copy(IccidInventory source){IccidInventory target=new IccidInventory();BeanUtils.copyProperties(source,target);return target;}

    private void history(IccidInventory i,String action,Long customerId,Long orderId,String reason,String operator){IccidAssignmentHistory h=new IccidAssignmentHistory();h.setIccidId(i.getId());h.setIccid(i.getIccid());h.setCustomerId(customerId);h.setOrderId(orderId);h.setActionType(action);h.setOperatorName(operator);h.setReason(reason);historyMapper.insert(h);}

    private String normalize(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("ICCID不能为空");String v=value.trim();if(!v.matches("[0-9]{15,22}"))throw new IllegalArgumentException("ICCID应为15至22位数字");return v;}

    private String first(Map<String,String> m,String...keys){for(String key:keys)if(notBlank(m.get(key)))return m.get(key);return null;} private boolean notBlank(String v){return v!=null&&!v.isBlank();} private boolean contains(String v,String q){return v!=null&&v.contains(q);}
}
