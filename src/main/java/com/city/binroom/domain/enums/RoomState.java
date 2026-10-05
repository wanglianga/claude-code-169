package com.city.binroom.domain.enums;

/** 设备/垃圾房运行状态 */
public enum RoomState {
    NORMAL("正常"),
    FULL("满溢"),
    ODOR("异味"),
    FULL_ODOR("满溢+异味"),
    OFFLINE("设备断网"),
    NIGHT_PEAK("夜间高峰");

    private final String label;

    RoomState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
