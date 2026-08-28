package com.cmhk.business.module.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 管理后台 Redis 缓存命名空间和筛选条件摘要工具。 */
public final class AdminCacheKeys {
    public static final String DASHBOARD = "cmhk:admin:dashboard:";
    public static final String CUSTOMERS = "cmhk:admin:customers:";
    public static final String CUSTOMER_CHANNELS = "cmhk:admin:customer-channels:";
    public static final String ORDERS = "cmhk:admin:orders:";
    public static final String ICCIDS = "cmhk:admin:iccids:";
    public static final String RESOURCES = "cmhk:admin:resources:";
    public static final String TASKS = "cmhk:admin:tasks:";

    private AdminCacheKeys() {
    }

    /** 将可能含手机号的查询条件转换为固定长度摘要，避免敏感值直接出现在 Redis 键中。 */
    public static String discriminator(Object... values) {
        StringBuilder source = new StringBuilder();
        for (Object value : values) {
            source.append(value == null ? "<null>" : value)
                    .append('|');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", ex);
        }
    }
}
