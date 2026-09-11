package com.fooddelivery.rbac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityRoleUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void toAuthority_nullOrBlank_defaultsToRoleUser() {
        assertEquals(AppRoles.AUTHORITY_USER, SecurityRoleUtils.toAuthority(null));
        assertEquals(AppRoles.AUTHORITY_USER, SecurityRoleUtils.toAuthority("   "));
    }

    @Test
    void toAuthority_addsRolePrefixWhenMissing() {
        assertEquals("ROLE_ADMIN", SecurityRoleUtils.toAuthority("admin"));
        assertEquals("ROLE_ADMIN", SecurityRoleUtils.toAuthority("ADMIN"));
    }

    @Test
    void toAuthority_preservesExistingRolePrefix() {
        assertEquals("ROLE_CUSTOM", SecurityRoleUtils.toAuthority("ROLE_CUSTOM"));
    }

    @Test
    void isAdmin_reflectsContextAuthorities() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "x",
                null,
                List.of(new SimpleGrantedAuthority(AppRoles.AUTHORITY_ADMIN))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityRoleUtils.isAdmin());
        assertTrue(SecurityRoleUtils.hasAuthority(AppRoles.AUTHORITY_ADMIN));
        assertFalse(SecurityRoleUtils.isRestaurantOwner());
    }

    @Test
    void hasAuthority_emptyContext_returnsFalse() {
        assertFalse(SecurityRoleUtils.hasAuthority(AppRoles.AUTHORITY_USER));
    }
}
