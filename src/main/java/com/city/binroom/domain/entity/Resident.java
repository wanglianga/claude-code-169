package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 居民 */
@Getter
@Setter
@Entity
@Table(name = "resident")
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 刷卡卡号 / 扫码虚拟卡号 */
    @Column(nullable = false, unique = true)
    private String cardNo;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    private String phone;

    /** 积分余额（与积分流水合计保持一致） */
    @Column(nullable = false)
    private Integer pointsBalance = 0;
}
