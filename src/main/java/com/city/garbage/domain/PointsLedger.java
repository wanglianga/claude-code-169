package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 积分流水：每一笔积分变动都与真实投放记录对应 */
@Getter
@Setter
@Entity
@Table(name = "points_ledger", indexes = {
        @Index(name = "idx_ledger_resident", columnList = "resident_id")
})
public class PointsLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "disposal_id")
    private Long disposalId;

    @Column(nullable = false)
    private Integer changePoints;

    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(length = 128)
    private String reason;

    private OffsetDateTime createdAt;

    public PointsLedger() {}

    public PointsLedger(Long residentId, Long disposalId, Integer changePoints,
                        Integer balanceAfter, String reason, OffsetDateTime createdAt) {
        this.residentId = residentId;
        this.disposalId = disposalId;
        this.changePoints = changePoints;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.createdAt = createdAt;
    }
}
