package com.cmhk.business.module.admin;

import com.cmhk.business.config.AdminProperties;
import com.cmhk.business.module.admin.security.AdminTokenService;
import com.cmhk.business.module.admin.security.AdminPrincipal;
import com.cmhk.business.module.admin.entity.AdminUser;
import com.cmhk.business.module.admin.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class AdminTokenServiceTests {
    @Test void loginAndVerifyAdminToken() {
        AdminProperties properties = new AdminProperties();
        properties.setTokenSecret("a-very-long-test-token-secret");
        AdminUserService users = mock(AdminUserService.class);
        AdminUser user = new AdminUser(); user.setId(1L); user.setUsername("admin"); user.setRoleCode("ADMIN"); user.setScopeType("ALL");
        when(users.authenticate("admin", "strong-password")).thenReturn(user);
        when(users.findActivePrincipal(1L)).thenReturn(java.util.Optional.of(new AdminPrincipal(1L,"admin","ADMIN","ALL",null)));
        when(users.authenticate("admin", "wrong")).thenThrow(new IllegalArgumentException("管理员账号或密码错误"));
        AdminTokenService service = new AdminTokenService(properties, users);
        String token = service.login("admin", "strong-password");
        assertEquals("admin", service.verify(token).orElseThrow().username());
        assertTrue(service.verify(token + "x").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.login("admin", "wrong"));
    }
}
