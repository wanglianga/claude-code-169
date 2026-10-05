package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.PointsDirection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 积分流水：每一笔积分变动都与一次投放或补发动作对应 */
@Getter
@Setter
@Entity
@Table(name = "points_ledger")
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resident resident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PointsDirection direction;

    @Column(nullable = false)
    private Integer points;

    /** 关联投放单号 */
    private String disposalBizNo;

    @Column(length = 300)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
