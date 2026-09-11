package com.fooddelivery.rbac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RbacSupportTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireUserId_whenDetailsIsLong_returnsId() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u", null, List.of());
        auth.setDetails(77L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(77L, RbacSupport.requireUserId());
    }

    @Test
    void requireUserId_whenMissing_throwsUnauthorized() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, RbacSupport::requireUserId);
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void assertSelfOrAdmin_adminMayAccessAnyUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority(AppRoles.AUTHORITY_ADMIN))
        );
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        RbacSupport.assertSelfOrAdmin(999L, "orders");
    }

    @Test
    void assertSelfOrAdmin_nonAdminSameUser_allowed() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u", null, List.of());
        auth.setDetails(5L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        RbacSupport.assertSelfOrAdmin(5L, "profile");
    }

    @Test
    void assertSelfOrAdmin_nonAdminDifferentUser_forbidden() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u", null, List.of());
        auth.setDetails(5L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> RbacSupport.assertSelfOrAdmin(9L, "profile")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
