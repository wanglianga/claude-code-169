package com.city.binroom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 联动规则参数（application.yml 中 app.linkage 前缀） */
@Data
@Component
@ConfigurationProperties(prefix = "app.linkage")
public class LinkageProperties {
    private int fullThresholdPct = 90;
    private int odorThreshold = 60;
    private int nightStartHour = 22;
    private int nightEndHour = 6;
    private int nightPeakWindowMinutes = 30;
    private int nightPeakCount = 3;
    private int offlineMinutes = 5;
    private int fullEscalateMinutes = 15;
    private int postPickupCleanMinutes = 30;
    private int pointsCreditTimeoutSeconds = 60;
    private int cleaningTimeoutMinutes = 60;
    private int pickupTimeoutMinutes = 90;
}
