package com.city.garbage.event;

/**
 * 设备上报数据的类型。
 */
public enum DeviceEventType {
    /** 厨余桶满溢 */
    KITCHEN_FULL,
    /** 其他垃圾容量 */
    OTHER_CAPACITY,
    /** 异味传感器读数 */
    ODOR,
    /** 门禁开合 */
    ACCESS,
    /** 摄像头识别结果 */
    CAMERA_DETECTION,
    /** 称重 */
    WEIGHT,
    /** 消杀设备状态 */
    DISINFECTION,
    /** 清运车到场/离场 */
    TRUCK_ARRIVAL,
    /** 心跳 */
    HEARTBEAT
}
