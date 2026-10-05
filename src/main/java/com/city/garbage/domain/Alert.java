package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 设备/业务告警。告警不直接派单，而是先并入“治理联动单”，
 * 满溢、异味、错投在同一垃圾房同时出现时只会形成一张联动单。
 */
@Getter
@Setter
@Entity
@Table(name = "alert", indexes = {
        @Index(name = "idx_alert_room_status", columnList = "room_code,status"),
        @Index(name = "idx_alert_case", columnList = "case_no")
})
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "room_code", nullable = false, length = 32)
    private String roomCode;

    @Column(name = "community_id")
    private Long communityId;

    /** 错投/积分类告警关联的投放记录 */
    @Column(name = "disposal_id")
    private Long disposalId;

    /** 投诉类告警关联的投诉 */
    @Column(name = "complaint_id")
    private Long complaintId;

    @Column(name = "case_no", length = 40)
    private String caseNo;

    @Column(length = 255)
    private String message;

    /** 同类型未闭环告警去重键，如 NIGHT_PEAK:ROOM1:2026-10-05 */
    @Column(name = "dedupe_key", length = 120)
    private String dedupeKey;

    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    @Column(length = 255)
    private String resolveNote;
}
