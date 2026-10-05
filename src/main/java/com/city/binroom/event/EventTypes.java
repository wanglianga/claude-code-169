package com.city.binroom.event;

/** 联动消息类型 */
public final class EventTypes {
    public static final String DEVICE_REPORTED = "DEVICE_REPORTED";
    public static final String DISPOSAL_CREATED = "DISPOSAL_CREATED";
    public static final String COMPLAINT_CREATED = "COMPLAINT_CREATED";
    public static final String PICKUP_ARRIVED = "PICKUP_ARRIVED";
    public static final String PICKUP_COMPLETED = "PICKUP_COMPLETED";
    public static final String CLEANING_COMPLETED = "CLEANING_COMPLETED";
    public static final String RECTIFICATION_REVIEWED = "RECTIFICATION_REVIEWED";
    public static final String POINTS_CREDITED = "POINTS_CREDITED";

    private EventTypes() {
    }
}
