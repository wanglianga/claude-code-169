package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.AccessMethod;
import com.city.binroom.domain.enums.AiVerdict;
import com.city.binroom.domain.enums.WasteType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 居民一次投放记录：小区、楼栋、垃圾类型、重量、积分、违规提示
 * 是保洁/清运/积分/投诉/整改联动的事实依据。
 */
@Getter
@Setter
@Entity
@Table(name = "disposal_record")
public class DisposalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外展示的业务单号 */
    @Column(nullable = false, unique = true, length = 40)
    private String bizNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WasteType wasteType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccessMethod accessMethod;

    /** 重量 kg */
    @Column(nullable = false)
    private Double weightKg;

    /** 摄像头 AI 结论 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiVerdict aiVerdict = AiVerdict.UNKNOWN;

    /** 给居民的违规提示文案 */
    @Column(length = 500)
    private String violationTip;

    /** 本次应得积分 */
    @Column(nullable = false)
    private Integer basePoints = 0;

    /** 违规扣分（正数） */
    @Column(nullable = false)
    private Integer penaltyPoints = 0;

    /** 积分是否已到账 */
    @Column(nullable = false)
    private Boolean pointsCredited = false;

    /** 积分是否因疑似错投被暂扣（区别于“积分未到账”故障） */
    @Column(nullable = false)
    private Boolean pointsWithheld = false;

    /** 是否已执行违规扣分（防止申诉返还后重复处理） */
    @Column(nullable = false)
    private Boolean penaltyApplied = false;

    /** 积分结论是否已最终结算（到账 / 没收 / 扣分），未结算且未暂扣的为“积分未到账” */
    @Column(nullable = false)
    private Boolean pointsSettled = false;

    /** 模拟积分链路延迟/丢失（设备断网场景演示用）：入账处理器跳过，由超时巡检补发 */
    @Column(nullable = false)
    private Boolean creditDelayed = false;

    /** 巡检是否已发现该笔积分异常（防止重复告警） */
    @Column(nullable = false)
    private Boolean pointsAnomalyNoticed = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
