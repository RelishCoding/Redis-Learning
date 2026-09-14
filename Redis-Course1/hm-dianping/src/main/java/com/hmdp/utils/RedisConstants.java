package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "hmdp:login:code:";
    public static final Long LOGIN_CODE_TTL = 5L;

    public static final String LOGIN_USER_KEY = "hmdp:login:token:";
    public static final Long LOGIN_USER_TTL = 60L;

    public static final String SHOP_INFO_KEY = "hmdp:shop:info:";
    public static final Long SHOP_INFO_TTL = 30L;

    public static final String SHOP_TYPE_KEY = "hmdp:shop:type";

    public static final Long CACHE_NULL_TTL = 2L;

    public static final String LOCK_SHOP_KEY = "hmdp:lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
}
