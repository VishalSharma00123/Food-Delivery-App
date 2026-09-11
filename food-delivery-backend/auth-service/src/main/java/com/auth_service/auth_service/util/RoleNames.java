package com.auth_service.auth_service.util;

public final class RoleNames {

    private RoleNames() {
    }

    /** DB id 1 — base account role; extend with domain roles in JWT. */
    public static final String ROLE_USER = "ROLE_USER";

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_RESTAURANT_OWNER = "ROLE_RESTAURANT_OWNER";
    /** DB name {@code DELIVERY_PARTNER} (id 4). */
    public static final String ROLE_DELIVERY_PARTNER = "ROLE_DELIVERY_PARTNER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}