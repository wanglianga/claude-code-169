package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 垃圾房状态聚合。
 * 物业、环卫、街道、居民四端围绕同一条记录看到相同状态。
 */
@Getter
@Setter
@Entity
@Table(name = "bin_room")
public class BinRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Community community;

    /** 垃圾房编号 */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String address;

    /** 厨余桶容量百分比 0-100 */
    @Column(nullable = false)
    private Integer kitchenFillPct = 0;

    /** 其他垃圾桶容量百分比 0-100 */
    @Column(nullable = false)
    private Integer otherFillPct = 0;

    /** 异味传感器数值 0-100 */
    @Column(nullable = false)
    private Integer odorLevel = 0;

    /** 设备是否在线（心跳驱动） */
    @Column(nullable = false)
    private Boolean online = true;

    private LocalDateTime lastHeartbeatAt;

    /** 厨余桶进入满溢的时间（用于“满溢但清运车未到”计时） */
    private LocalDateTime kitchenFullSince;

    /** 清运车是否在现场 */
    @Column(nullable = false)
    private Boolean truckOnSite = false;

    /** 最近一次清运完成时间 */
    private LocalDateTime lastPickupAt;

    /** 最近一次保洁/消杀完成时间 */
    private LocalDateTime lastCleanedAt;

    /** 是否处于夜间投放高峰 */
    @Column(nullable = false)
    private Boolean nightPeak = false;

    private LocalDateTime updatedAt;
}
