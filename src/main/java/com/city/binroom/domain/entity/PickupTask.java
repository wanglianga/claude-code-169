package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.enums.WasteType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 环卫清运任务（环卫端按优先级处理） */
@Getter
@Setter
@Entity
@Table(name = "pickup_task")
public class PickupTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String taskNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WasteType wasteType;

    /** 优先级评分，越大越紧急（环卫端排序依据） */
    @Column(nullable = false)
    private Integer priorityScore = 0;

    @Column(length = 500)
    private String reason;

    @Column(length = 32)
    private String linkageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status = TaskStatus.PENDING;

    private String assignee;

    /** 建议清运路线 */
    private String routeCode;

    private LocalDateTime expectedBefore;
    private LocalDateTime assignedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;

    /** 实际清运重量 kg */
    private Double pickedWeightKg;

    @Column(length = 500)
    private String resultNote;

    /** 清运后是否已完成清洁（联动“清运后未清洁”场景） */
    @Column(nullable = false)
    private Boolean cleanedAfterPickup = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean escalated = false;
}
