package com.city.garbage.domain;

public enum TruckVisitStatus {
    ARRIVED,   // 到场
    DEPARTED,  // 离场（清运完成）
    CONFIRMED  // 离场且垃圾房已清洁确认
}
