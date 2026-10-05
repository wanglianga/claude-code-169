package com.city.garbage.kafka;

import com.city.garbage.event.DeviceEvent;
import com.city.garbage.service.DeviceIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/** 消费设备上报事件，进入联动引擎。消费组 garbage-room-linkage。 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class DeviceEventConsumer {

    private final DeviceIngestService ingestService;

    @KafkaListener(topics = KafkaTopicConfig.DEVICE_EVENTS, groupId = "garbage-room-linkage")
    public void onDeviceEvent(@Payload DeviceEvent event,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        log.info("收到设备事件 type={} room={} partition={}",
                event.eventType(), event.roomCode(), partition);
        ingestService.ingest(event);
    }
}
