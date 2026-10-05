package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 各端工作人员账号：物业 / 环卫 / 街道 / 管理员 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    /** PROPERTY / SANITATION / STREET / ADMIN */
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String displayName;

    /** 所属小区（可空，表示街道/管理员可看全部） */
    private Long communityId;
}
