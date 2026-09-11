package com.fooddelivery.rbac;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityRoleUtils {

    private SecurityRoleUtils() {
    }

    public static String toAuthority(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return AppRoles.AUTHORITY_USER;
        }
        String upper = roleName.trim().toUpperCase();
        if (upper.startsWith("ROLE_")) {
            return upper;
        }
        return "ROLE_" + upper;
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin() {
        return hasAuthority(AppRoles.AUTHORITY_ADMIN);
    }

    public static boolean isRestaurantOwner() {
        return hasAuthority(AppRoles.AUTHORITY_RESTAURANT_OWNER);
    }

    public static boolean isDeliveryPartner() {
        return hasAuthority(AppRoles.AUTHORITY_DELIVERY_PARTNER);
    }

    public static boolean isCustomerOrUser() {
        return hasAuthority(AppRoles.AUTHORITY_CUSTOMER) || hasAuthority(AppRoles.AUTHORITY_USER);
    }
}
