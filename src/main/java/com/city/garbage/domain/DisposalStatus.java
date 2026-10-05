package com.city.garbage.domain;

public enum DisposalStatus {
    PENDING_POINTS,  // 已投放，积分待结算
    SETTLED,         // 积分已到账
    APPEALING,       // 居民申诉中（摄像头误判）
    APPEAL_ACCEPTED, // 申诉成立，违规撤销并退还积分
    APPEAL_REJECTED  // 申诉驳回，维持违规
}
