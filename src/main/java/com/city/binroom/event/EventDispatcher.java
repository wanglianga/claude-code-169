package com.city.binroom.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 统一事件分发：local 与 kafka 两种入口最终都汇聚到这里，
 * 保证“同一套联动逻辑”不依赖投递机制。
 * 每个处理器在独立的新事务中执行：事件通常在上游事务提交后触发
 * （afterCommit / Kafka 消费线程），处理器内的任务与留痕必须独立提交。
 */
@Slf4j
@Component
public class EventDispatcher {

    private final List<LinkageProcessor> processors;
    private final TransactionTemplate newTxTemplate;

    public EventDispatcher(List<LinkageProcessor> processors, PlatformTransactionManager txManager) {
        this.processors = processors;
        this.newTxTemplate = new TransactionTemplate(txManager);
        this.newTxTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void dispatch(LinkageMessage message) {
        for (LinkageProcessor processor : processors) {
            if (processor.supports(message)) {
                log.info("联动处理 type={} eventId={} -> {}",
                        message.getType(), message.getEventId(),
                        processor.getClass().getSimpleName());
                newTxTemplate.executeWithoutResult(status -> {
                    try {
                        processor.handle(message);
                    } catch (Exception e) {
                        log.error("联动处理器 {} 执行失败 type={} eventId={}: {}",
                                processor.getClass().getSimpleName(), message.getType(),
                                message.getEventId(), e.getMessage(), e);
                        throw e;
                    }
                });
            }
        }
    }
}
