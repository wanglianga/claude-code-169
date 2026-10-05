package com.city.binroom.domain.enums;

/** 积分变动方向 */
public enum PointsDirection {
    EARN("获得"),
    PENALTY("扣减"),
    MAKEUP("补发");

    private final String label;

    PointsDirection(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
