package com.city.garbage.domain;

public enum ComplaintStatus {
    OPEN,        // 待处理
    LINKED,      // 已并入联动单
    RESOLVED     // 已反馈闭环
}
