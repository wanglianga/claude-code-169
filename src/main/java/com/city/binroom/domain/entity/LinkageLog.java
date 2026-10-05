package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 联动日志：记录每一次“设备告警 → 现场治理动作”的关联。
 * 九类场景的每一条触发、升级、完成都在此留痕，保证积分/投诉/整改
 * 与实际投放和清运记录可对应、可追溯。
 */
@Getter
@Setter
@Entity
@Table(name = "linkage_log",
        indexes = @Index(name = "idx_linkage_room", columnList = "bin_room_id,created_at"))
public class LinkageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    /** LinkageType */
    @Column(nullable = false, length = 32)
    private String linkageType;

    /** TRIGGER / ESCALATE / COMPLETE / CANCEL / CREDIT / REVERSE */
    @Column(nullable = false, length = 16)
    private String action;

    @Column(nullable = false, length = 1000)
    private String detail;

    /** 关联业务单号（投放/保洁/清运/投诉/整改/积分） */
    private String refBizNo;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
