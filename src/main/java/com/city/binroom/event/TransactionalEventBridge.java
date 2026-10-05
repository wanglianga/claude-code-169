package com.city.binroom.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后投递。两种 profile 共用同一个监听器，按 app.eventbus.mode 决定去向：
 * - local：直接交给 EventDispatcher 在进程内处理
 * - kafka：发送到 Kafka topic，由 KafkaLinkageListener 消费后再交给 EventDispatcher
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalEventBridge {

    private final EventDispatcher dispatcher;
    private final KafkaTemplate<String, LinkageMessage> kafkaTemplate;

    @Value("${app.eventbus.mode}")
    private String mode;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(AfterCommitEvent event) {
        LinkageMessage msg = event.getMessage();
        if ("kafka".equalsIgnoreCase(mode)) {
            kafkaTemplate.send(event.getTopic(), msg.getEventId(), msg)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 发送失败 topic={} eventId={}", event.getTopic(), msg.getEventId(), ex);
                        } else {
                            log.debug("Kafka 已发送 topic={} type={} eventId={}",
                                    event.getTopic(), msg.getType(), msg.getEventId());
                        }
                    });
        } else {
            dispatcher.dispatch(msg);
        }
    }
}
