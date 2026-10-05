package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 居民投诉（如异味投诉），投诉必须并入对应垃圾房联动单并随整改闭环 */
@Getter
@Setter
@Entity
@Table(name = "complaint", indexes = {
        @Index(name = "idx_complaint_community", columnList = "community_id"),
        @Index(name = "idx_complaint_status", columnList = "status"),
        @Index(name = "idx_complaint_created", columnList = "created_at")
})
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "room_code", length = 32)
    private String roomCode;

    @Column(name = "resident_id")
    private Long residentId;

    @Column(length = 32)
    private String contact;

    @Column(nullable = false, length = 32)
    private String type = "ODOR";

    @Column(nullable = false, length = 512)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "case_no", length = 40)
    private String caseNo;

    @Column(name = "reply", length = 512)
    private String reply;

    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
}
