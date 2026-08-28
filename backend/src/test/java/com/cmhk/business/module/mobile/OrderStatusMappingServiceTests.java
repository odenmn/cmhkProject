package com.cmhk.business.module.mobile;

import com.cmhk.business.module.mobile.entity.OrderStatusCode;
import com.cmhk.business.module.mobile.service.OrderStatusMappingService;
import com.cmhk.business.module.mobile.service.impl.OrderStatusMappingServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 CMHK 状态映射和未知状态保护。 */
class OrderStatusMappingServiceTests {

    private final OrderStatusMappingService service = new OrderStatusMappingServiceImpl();

    @Test
    void activatedStatusHasHighestPriority() {
        OrderStatusMappingService.MappingResult result = service.map(
                null,
                "审核通过",
                "无",
                "已激活");

        assertEquals(OrderStatusCode.ACTIVATED.name(), result.standardStatus());
        assertFalse(result.unknown());
    }

    @Test
    void supplementStatusMapsToNeedSupplement() {
        OrderStatusMappingService.MappingResult result = service.map(
                null,
                "审核中",
                "需要补地址说明",
                null);

        assertEquals(OrderStatusCode.NEED_SUPPLEMENT.name(), result.standardStatus());
        assertFalse(result.unknown());
    }

    @Test
    void unknownStatusDoesNotProduceStandardStatus() {
        OrderStatusMappingService.MappingResult result = service.map(
                "神秘状态",
                null,
                null,
                null);

        assertNull(result.standardStatus());
        assertTrue(result.unknown());
    }

    @Test
    void emptyStatusLeavesCurrentOrderStateUntouched() {
        OrderStatusMappingService.MappingResult result = service.map(null, null, null, null);

        assertNull(result.standardStatus());
        assertFalse(result.unknown());
    }

    @Test
    void neutralSupplementStatusDoesNotCreateFalseException() {
        OrderStatusMappingService.MappingResult result = service.map(null, null, "无需补件", null);

        assertNull(result.standardStatus());
        assertFalse(result.unknown());
    }

    @Test
    void notActivatedDoesNotMapToActivated() {
        OrderStatusMappingService.MappingResult result = service.map(null, null, null, "not activated");

        assertEquals(OrderStatusCode.WAITING_ACTIVATION.name(), result.standardStatus());
    }
}
