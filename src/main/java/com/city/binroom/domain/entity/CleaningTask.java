package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 物业保洁/消杀任务（物业端待处理任务） */
@Getter
@Setter
@Entity
@Table(name = "cleaning_task")
public class CleaningTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String taskNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    /** CLEANING(保洁) / DISINFECT(消杀) / POST_PICKUP_CLEAN(清运后清洁) */
    @Column(nullable = false, length = 32)
    private String taskKind;

    @Column(length = 500)
    private String reason;

    /** 触发来源联动类型 */
    @Column(length = 32)
    private String linkageType;

    /** 来源业务单号（如清运后清洁对应清运单号） */
    @Column(length = 40)
    private String sourceTaskNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status = TaskStatus.PENDING;

    private String assignee;

    private LocalDateTime expectedBefore;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    @Column(length = 500)
    private String resultNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 是否升级到街道 */
    @Column(nullable = false)
    private Boolean escalated = false;
}
