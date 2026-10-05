package com.city.binroom.domain.enums;

/** 通用任务/事件处理状态 */
public enum TaskStatus {
    PENDING("待处理"),
    DISPATCHED("已派单"),
    IN_PROGRESS("处理中"),
    DONE("已完成"),
    OVERDUE("已超时"),
    ESCALATED("已升级"),
    CANCELLED("已取消");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
