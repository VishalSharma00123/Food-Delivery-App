package com.api_gateway.api_gateway.filter;

/**
 * Exchange attribute keys for identity data validated by {@link JwtAuthenticationFilter}
 * and re-applied by {@link IdentityForwardingFilter} immediately before the downstream call.
 */
public final class IdentityHeadersBridge {

    public static final String AUTH_HEADER = "bitecraft.authHeader";
    public static final String USER_ID = "bitecraft.userId";
    public static final String USER_EMAIL = "bitecraft.userEmail";
    public static final String USER_ROLES = "bitecraft.userRoles";

    private IdentityHeadersBridge() {
    }
}
