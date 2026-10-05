package com.city.binroom.event;

/** 联动处理器：每种业务域一个实现，由分发器统一调度 */
public interface LinkageProcessor {

    boolean supports(LinkageMessage message);

    void handle(LinkageMessage message);
}
