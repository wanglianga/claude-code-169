package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 小区 */
@Getter
@Setter
@Entity
@Table(name = "community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 小区名称 */
    @Column(nullable = false)
    private String name;

    /** 所属街道 */
    @Column(nullable = false)
    private String street;

    /** 物业名称 */
    private String propertyCompany;
}
