package com.city.binroom.event;

import org.springframework.context.ApplicationEvent;

/** Spring 事务事件桥：在事务提交后再投递（local 直发处理器 / kafka 发往 broker） */
public class AfterCommitEvent extends ApplicationEvent {

    private final String topic;
    private final LinkageMessage message;

    public AfterCommitEvent(Object source, String topic, LinkageMessage message) {
        super(source);
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public LinkageMessage getMessage() {
        return message;
    }
}
