package com.city.binroom.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 登录令牌（工作人员或居民） */
@Getter
@Setter
@Entity
@Table(name = "auth_token")
public class AuthToken {

    @Id
    private String token;

    /** RESIDENT / PROPERTY / SANITATION / STREET / ADMIN */
    @Column(nullable = false)
    private String role;

    private Long userId;

    private Long residentId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
