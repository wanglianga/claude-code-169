package com.city.binroom.domain.entity;

import com.city.binroom.domain.enums.ComplaintType;
import com.city.binroom.domain.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 居民/街道侧投诉（异味投诉等），可触发保洁消杀联动 */
@Getter
@Setter
@Entity
@Table(name = "complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String complaintNo;

    @ManyToOne(fetch = FetchType.LAZY)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BinRoom binRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplaintType type;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status = TaskStatus.PENDING;

    /** 处理该投诉的保洁任务 */
    @ManyToOne(fetch = FetchType.LAZY)
    private CleaningTask cleaningTask;

    private String handleNote;
    private LocalDateTime handledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
