package com.city.binroom.domain.enums;

/** 整改核查结果 */
public enum RectifyResult {
    PENDING("待整改"),
    PASS("整改合格"),
    FAIL("整改不合格"),
    WAIVED("误判撤销");

    private final String label;

    RectifyResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
