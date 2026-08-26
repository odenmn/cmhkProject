package com.cmhk.business.module.admin.service;

import java.util.Map;

/** 管理端首页统计能力的服务接口。 */
public interface AdminDashboardService {
    /** 汇总客户、订单、ICCID、对账异常和待结算数量。 */
    Map<String, Long> metrics();
}
