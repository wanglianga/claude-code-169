package com.city.garbage.domain;

/** 治理联动单状态 */
public enum CaseStatus {
    OPEN,            // 待处理（有未完成任务）
    IN_PROGRESS,     // 任务已派发/处置中
    RECTIFYING,      // 保洁清运完成，等待整改复核
    RESOLVED,        // 已闭环
    CLOSED_FALSE     // 申诉成立，误判关闭
}
