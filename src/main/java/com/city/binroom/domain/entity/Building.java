package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 楼栋 */
@Getter
@Setter
@Entity
@Table(name = "building")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Community community;

    /** 楼栋名称，如 3 号楼 */
    @Column(nullable = false)
    private String name;
}
