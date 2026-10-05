package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 垃圾房当前聚合状态：四类角色（物业/环卫/街道/居民）看到的是同一份状态。
 * 设备事件不断刷新该表，联动引擎据此产生告警与治理动作。
 */
@Getter
@Setter
@Entity
@Table(name = "garbage_room")
public class GarbageRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(length = 128)
    private String address;

    /* ---- 设备/桶态实时字段 ---- */
    private Integer kitchenCapacityPct = 0;
    private Integer otherCapacityPct = 0;
    private boolean kitchenFull = false;
    private Integer currentOdor = 0;
    private boolean doorOpen = false;
    private boolean truckPresent = false;
    private boolean online = true;
    private OffsetDateTime lastHeartbeatAt;
    private OffsetDateTime kitchenFullSince;
    private OffsetDateTime otherFullSince;
    private OffsetDateTime lastDisinfectionAt;
    private OffsetDateTime lastCollectionAt;
    private OffsetDateTime lastCleanedAt;

    /** 夜间投放计数（配合 nightCountDate 按日清零） */
    private Integer nightDisposalCount = 0;
    private String nightCountDate;

    /** 当前活跃的治理联动单号（无则为空），保证多告警合并到同一单 */
    @Column(length = 40)
    private String activeCaseNo;
}
