package com.city.garbage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 小区（街道 -> 小区 -> 楼栋 -> 垃圾房） */
@Getter
@Setter
@Entity
@Table(name = "community")
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    /** 所属街道 */
    @Column(nullable = false, length = 64)
    private String streetName;

    public Community() {}

    public Community(String code, String name, String streetName) {
        this.code = code;
        this.name = name;
        this.streetName = streetName;
    }
}
