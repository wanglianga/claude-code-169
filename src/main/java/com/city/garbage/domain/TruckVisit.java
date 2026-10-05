package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 清运车到场记录：到场 -> 清运离场 -> 清洁确认 */
@Getter
@Setter
@Entity
@Table(name = "truck_visit", indexes = {
        @Index(name = "idx_visit_room", columnList = "room_code")
})
public class TruckVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, length = 32)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TruckVisitStatus status;

    private Integer kitchenCapacityAtArrival;
    private Integer otherCapacityAtArrival;

    private OffsetDateTime arrivedAt;
    private OffsetDateTime departedAt;
    private OffsetDateTime cleanedConfirmedAt;
}
