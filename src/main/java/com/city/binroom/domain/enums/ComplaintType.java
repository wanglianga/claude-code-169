package com.city.binroom.domain.enums;

/** 投诉类型 */
public enum ComplaintType {
    ODOR("异味投诉"),
    OVERFLOW("满溢扰民"),
    NOISE("夜间噪音"),
    HYGIENE("卫生问题"),
    OTHER("其他投诉");

    private final String label;

    ComplaintType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
