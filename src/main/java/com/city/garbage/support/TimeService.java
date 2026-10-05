package com.city.garbage.support;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 统一时钟，测试可替换为固定时钟 */
@Component
public class TimeService {
    private final ZoneId zone = ZoneId.of("Asia/Shanghai");
    private Clock clock = Clock.system(zone);

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    public ZoneId zone() {
        return zone;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
