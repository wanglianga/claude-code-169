package com.city.garbage.kafka;

import com.city.garbage.event.DeviceEvent;
import com.city.garbage.service.DeviceIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 设备事件入口：启用 Kafka 时发布到 device-events，由消费者联动处理；
 * 未启用（本地/测试）时直接同步处理，保证无外部中间件也可完整运行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceEventGateway {

    private final KafkaTemplate<String, DeviceEvent> kafkaTemplate;
    private final DeviceIngestService ingestService;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public void publish(DeviceEvent event) {
        if (kafkaEnabled) {
            kafkaTemplate.send(KafkaTopicConfig.DEVICE_EVENTS, event.getRoomCode(), event)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.error("设备事件投递 Kafka 失败，降级同步处理 room={}", event.getRoomCode(), ex);
                            ingestService.ingest(event);
                        } else {
                            log.debug("设备事件已投递 Kafka room={} partition={}",
                                    event.getRoomCode(), res.getRecordMetadata().partition());
                        }
                    });
        } else {
            ingestService.ingest(event);
        }
    }
}
