package com.city.binroom.web.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 当前登录主体 */
@Data
@AllArgsConstructor
public class CurrentUser {
    public static final String ATTR_USER = "currentUser";

    private String token;
    private String role;
    private Long userId;
    private Long residentId;
}
