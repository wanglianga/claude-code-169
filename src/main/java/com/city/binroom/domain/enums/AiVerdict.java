package com.city.binroom.domain.enums;

/** 摄像头识别结论 */
public enum AiVerdict {
    CORRECT("投放正确"),
    MIS_SORT("错投"),
    SUSPECTED("疑似违规"),
    UNKNOWN("无法判定");

    private final String label;

    AiVerdict(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
