package com.evandorou.api;

/**
 * Header name for the opaque user id supplied by the caller (external identity system).
 */
public final class UserIdentityHeader {

    public static final String NAME = "X-User-Id";

    private UserIdentityHeader() {}
}
