package com.fooddelivery.rbac;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class RbacSupport {

    private RbacSupport() {
    }

    public static Long requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user id is required");
        }
        return userId;
    }

    public static void assertSelfOrAdmin(Long pathUserId, String resourceLabel) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        Long uid = requireUserId();
        if (!uid.equals(pathUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You may only access " + resourceLabel + " for your own account");
        }
    }
}
