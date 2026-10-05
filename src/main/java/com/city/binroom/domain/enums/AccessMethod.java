package com.city.binroom.domain.enums;

/** 居民投放时的门禁方式 */
public enum AccessMethod {
    CARD("刷卡"),
    QRCODE("扫码");

    private final String label;

    AccessMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
