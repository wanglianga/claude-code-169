package com.city.garbage.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 联动结果对外广播（四端大屏/通知服务可订阅 governance-events） */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovernanceEventPublisher {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public void publish(String roomCode, Map<String, Object> payload) {
        if (!kafkaEnabled) {
            log.debug("Kafka 未启用，跳过联动事件广播 room={}", roomCode);
            return;
        }
        try {
            kafkaTemplate.send(KafkaTopicConfig.GOVERNANCE_EVENTS, roomCode, payload);
        } catch (Exception e) {
            log.warn("联动事件广播失败 room={}: {}", roomCode, e.getMessage());
        }
    }
}
