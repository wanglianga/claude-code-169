package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.DeviceEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 设备上报原始事件 */
@Getter
@Setter
@Entity
@Table(name = "device_event",
        indexes = {
                @Index(name = "idx_device_event_room_time", columnList = "bin_room_id,event_time")
        })
public class DeviceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviceEventType eventType;

    /** 事件数值：容量百分比 / 异味值 / 重量(kg*1000 存小数) 等 */
    private Double numericValue;

    /** 垃圾类型（容量、摄像头识别时使用） */
    private String wasteType;

    /** 摄像头识别结论 / 附加信息，JSON 字符串 */
    @Column(length = 1000)
    private String payload;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false)
    private LocalDateTime receivedAt;
}
