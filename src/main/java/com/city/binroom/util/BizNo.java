package com.city.binroom.util;

import java.util.UUID;

/** 业务单号生成：前缀 + 毫秒时间戳 + 短随机串，避免高并发同毫秒冲突 */
public final class BizNo {

    private BizNo() {
    }

    public static String of(String prefix) {
        return prefix + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
