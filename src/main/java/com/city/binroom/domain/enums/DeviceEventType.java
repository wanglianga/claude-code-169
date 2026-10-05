package com.city.binroom.domain.enums;

/**
 * 设备上报事件类型：
 * 厨余桶满溢、其他垃圾容量、异味、门禁、摄像头识别、称重、消杀、清运车到场、心跳
 */
public enum DeviceEventType {
    KITCHEN_FULL("厨余桶满溢"),
    CAPACITY("桶容量"),
    ODOR("异味传感器"),
    ACCESS("门禁开合"),
    AI_RECOGNITION("摄像头识别"),
    WEIGH("称重"),
    DISINFECT("消杀设备"),
    TRUCK_ARRIVAL("清运车到场"),
    HEARTBEAT("设备心跳");

    private final String label;

    DeviceEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
