package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 设备原始上报留痕，便于排查“摄像头误判/设备断网”等争议 */
@Getter
@Setter
@Entity
@Table(name = "device_event_log", indexes = {
        @Index(name = "idx_devlog_room", columnList = "room_code")
})
public class DeviceEventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", length = 32)
    private String roomCode;

    @Column(name = "device_code", length = 64)
    private String deviceCode;

    @Column(name = "event_type", length = 32)
    private String eventType;

    @Column(length = 1024)
    private String payload;

    private OffsetDateTime occurredAt;
    private OffsetDateTime receivedAt;
}
