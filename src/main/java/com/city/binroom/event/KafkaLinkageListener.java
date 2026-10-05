package com.city.binroom.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 模式下的入站监听器：消费三个联动主题并交给统一分发器。
 * 处理器内部保证幂等，处理失败时不 ack 由 Kafka 重投。
 */
@Slf4j
@Component
@Profile("kafka")
public class KafkaLinkageListener {

    private final EventDispatcher dispatcher;

    public KafkaLinkageListener(EventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = {
            Topics.DEVICE_EVENTS,
            Topics.POINTS_EVENTS,
            Topics.LINKAGE_EVENTS
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, LinkageMessage> record, Acknowledgment ack) {
        LinkageMessage msg = record.value();
        try {
            log.debug("消费 Kafka 消息 topic={} type={} eventId={}",
                    record.topic(), msg.getType(), msg.getEventId());
            dispatcher.dispatch(msg);
            ack.acknowledge();
        } catch (Exception e) {
            // 不 ack，由 Kafka 重试；处理器已保证幂等
            log.error("联动消息处理失败，等待重试 type={} eventId={}: {}",
                    msg.getType(), msg.getEventId(), e.getMessage());
            throw e;
        }
    }
}
