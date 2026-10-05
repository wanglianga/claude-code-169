package com.city.garbage.domain;

/** 告警生命周期状态 */
public enum AlertStatus {
    OPEN,        // 已产生，待联动
    LINKED,      // 已并入治理联动单
    RESOLVED,    // 已闭环
    IGNORED      // 申诉成立等原因忽略
}
