package com.city.binroom.domain.enums;

/** 联动场景类型（九类治理场景） */
public enum LinkageType {
    FULL_NO_TRUCK("厨余满溢清运车未到"),
    MIS_SORT("错投识别"),
    NIGHT_PEAK("夜间投放高峰"),
    DEVICE_OFFLINE("设备断网"),
    ODOR_COMPLAINT("异味投诉"),
    NOT_CLEANED_AFTER_PICKUP("清运后未清洁"),
    POINTS_MISSING("积分未到账"),
    AI_APPEAL("摄像头误判申诉"),
    ODOR_ALERT("异味告警联动消杀");

    private final String label;

    LinkageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
