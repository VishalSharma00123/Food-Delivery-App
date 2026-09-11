package com.fooddelivery.rbac;

/**
 * Role names as in the auth DB / JWT {@code roles} claim (no {@code ROLE_} prefix).
 */
public final class AppRoles {

    private AppRoles() {
    }

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String RESTAURANT_OWNER = "RESTAURANT_OWNER";
    public static final String DELIVERY_PARTNER = "DELIVERY_PARTNER";
    public static final String CUSTOMER = "CUSTOMER";

    public static final String AUTHORITY_USER = "ROLE_USER";
    public static final String AUTHORITY_ADMIN = "ROLE_ADMIN";
    public static final String AUTHORITY_RESTAURANT_OWNER = "ROLE_RESTAURANT_OWNER";
    public static final String AUTHORITY_DELIVERY_PARTNER = "ROLE_DELIVERY_PARTNER";
    public static final String AUTHORITY_CUSTOMER = "ROLE_CUSTOMER";
}
