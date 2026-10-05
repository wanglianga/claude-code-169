package com.city.binroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 联动巡检定时器（可通过 app.sweep.enabled=false 关闭，测试中手动调用 runSweep） */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sweep.enabled", havingValue = "true", matchIfMissing = true)
public class LinkageSweepScheduler {

    private final LinkageSweepService sweepService;

    @Scheduled(fixedDelayString = "${app.sweep.fixed-delay-ms:30000}",
            initialDelayString = "${app.sweep.initial-delay-ms:15000}")
    public void tick() {
        try {
            sweepService.runSweep();
        } catch (Exception e) {
            log.warn("联动巡检异常: {}", e.getMessage());
        }
    }
}
