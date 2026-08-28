package com.cmhk.business.module.resource.service;

import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.resource.entity.ReferralChain;
import com.cmhk.business.module.resource.entity.ReferralNumber;
import com.cmhk.business.module.resource.entity.ReferralNumberHistory;
import com.cmhk.business.module.mobile.entity.MobilePlanOrder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 多接龙推荐号码业务。 */
public interface ReferralNumberService {
    List<Map<String, Object>> chains();

    /** 返回一条接龙从初始号码到当前龙头的完整顺序。 */
    Map<String, Object> trace(Long chainId);
    List<Map<String, Object>> numbers(Long chainId, String status, String keyword);
    List<MobilePlanOrder> eligibleOrders();
    ReferralChain createChain(String name, String initialReferralNumber, String remark, AdminPrincipal principal);
    ReferralChain changeChainStatus(Long chainId, String status, String reason, AdminPrincipal principal);
    ReferralNumber addCandidate(Long chainId, String number, String sourceReference, AdminPrincipal principal);
    Map<String, Object> previewImport(Long chainId, MultipartFile file);
    Map<String, Object> confirmImport(Long chainId, MultipartFile file, String expectedHash, AdminPrincipal principal);
    ReferralNumber designateHead(Long chainId, Long numberId, String reason, AdminPrincipal principal);
    ReferralNumber reserve(Long chainId, Long orderId, String reason, AdminPrincipal principal);
    ReferralNumber release(Long numberId, String reason, AdminPrincipal principal);
    ReferralNumber completeOnboarding(Long numberId, String reason, AdminPrincipal principal);
    ReferralNumber disable(Long numberId, String reason, AdminPrincipal principal);
    List<ReferralNumberHistory> history(Long numberId);
    Map<String, Object> orderResources(Long orderId);
    Map<String, Object> diagnostics();
}
