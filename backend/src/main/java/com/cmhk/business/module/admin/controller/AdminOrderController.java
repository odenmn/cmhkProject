package com.cmhk.business.module.admin.controller;
import com.cmhk.business.common.ApiResponse; import com.cmhk.business.config.AdminAuthInterceptor; import com.cmhk.business.module.admin.service.AdminOrderService; import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/admin/orders")
public class AdminOrderController {
   private static final Logger log= LoggerFactory.getLogger(AdminOrderController.class);
   private final AdminOrderService service;
   public AdminOrderController(AdminOrderService service){
       this.service=service;
   }
 @GetMapping
 public ApiResponse<List<MobilePlanOrder>> list(@RequestParam(required=false) String keyword,@RequestParam(required=false) String status,@RequestParam(required=false) Long customerId){
       var rows=service.list(keyword,status,customerId);log.info("管理端查询订单完成，数量={}",rows.size());
       return ApiResponse.success(rows);}
 @GetMapping("/{id}")
 public ApiResponse<MobilePlanOrder> detail(@PathVariable Long id){
       return ApiResponse.success(service.required(id));}
 @PostMapping
 public ApiResponse<MobilePlanOrder> create(@RequestBody MobilePlanOrder input,@RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator){
       return ApiResponse.success(
         service.save(null,input,operator));
   }
 @PutMapping("/{id}")
 public ApiResponse<MobilePlanOrder> update(@PathVariable Long id,@RequestBody MobilePlanOrder input,@RequestAttribute(AdminAuthInterceptor.ADMIN_USERNAME) String operator){
       return ApiResponse.success(service.save(id,input,operator));
   }
}
