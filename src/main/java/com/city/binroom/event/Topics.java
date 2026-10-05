package com.city.binroom.event;

/** 事件主题 */
public final class Topics {
    /** 设备上报事件 */
    public static final String DEVICE_EVENTS = "binroom.device-events";
    /** 居民投放与积分事件 */
    public static final String POINTS_EVENTS = "binroom.points-events";
    /** 投诉 / 清运 / 保洁 / 整改等联动事件 */
    public static final String LINKAGE_EVENTS = "binroom.linkage-events";

    private Topics() {
    }
}
