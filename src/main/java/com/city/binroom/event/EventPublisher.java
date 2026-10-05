package com.city.binroom.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一事件入口。业务代码只依赖该发布器：
 * - local 模式：事务提交后直接进入本地处理器
 * - kafka 模式：事务提交后发送到 Kafka，由消费组处理
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public LinkageMessage publish(String topic, String type, JsonNode payload) {
        LinkageMessage msg = new LinkageMessage(
                UUID.randomUUID().toString(), type, payload, LocalDateTime.now());
        springPublisher.publishEvent(new AfterCommitEvent(this, topic, msg));
        return msg;
    }
}
