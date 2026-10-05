package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.RectifyResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 整改单：错投违规、摄像头误判申诉共用。
 * 与投放记录一一对应，核查结果决定扣分是否撤销 / 积分是否补发。
 */
@Getter
@Setter
@Entity
@Table(name = "rectification")
public class Rectification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String rectifyNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DisposalRecord disposal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    /** VIOLATION(违规整改) / APPEAL(误判申诉) */
    @Column(nullable = false, length = 16)
    private String kind;

    @Column(length = 1000)
    private String appealReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RectifyResult result = RectifyResult.PENDING;

    @Column(length = 1000)
    private String reviewNote;

    private String reviewer;
    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
