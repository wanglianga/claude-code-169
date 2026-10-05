package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 居民：刷卡/扫码身份，积分余额 */
@Getter
@Setter
@Entity
@Table(name = "resident")
public class Resident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String cardNo;

    @Column(length = 64)
    private String name;

    @Column(name = "community_id")
    private Long communityId;

    /** 楼栋，如 12栋3单元 */
    @Column(length = 64)
    private String buildingName;

    @Column(nullable = false)
    private Integer totalPoints = 0;

    public Resident() {}

    public Resident(String cardNo, String name, Long communityId, String buildingName) {
        this.cardNo = cardNo;
        this.name = name;
        this.communityId = communityId;
        this.buildingName = buildingName;
    }
}
