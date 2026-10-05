package com.city.binroom.domain.enums;

/** 垃圾类型 */
public enum WasteType {
    KITCHEN("厨余垃圾"),
    OTHER("其他垃圾"),
    RECYCLABLE("可回收物"),
    HAZARDOUS("有害垃圾");

    private final String label;

    WasteType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
