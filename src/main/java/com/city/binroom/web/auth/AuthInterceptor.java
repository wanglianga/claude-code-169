package com.city.binroom.web.auth;

import com.city.binroom.domain.entity.AuthToken;
import com.city.binroom.domain.repo.AuthTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/** Token 鉴权与角色校验（演示系统，token 为随机不透明串，落库） */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> PERMIT_PREFIXES = Set.of(
            "/api/auth/", "/api/meta/", "/api/devices/",
            "/swagger-ui", "/v3/api-docs", "/h2-console");

    private final AuthTokenRepository tokenRepo;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPermitted(path)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.sendError(401, "未登录或缺少令牌");
            return false;
        }
        String tokenValue = auth.substring(7).trim();
        AuthToken token = tokenRepo.findById(tokenValue).orElse(null);
        if (token == null) {
            response.sendError(401, "令牌无效或已过期");
            return false;
        }
        String role = token.getRole();
        boolean allowed;
        if (path.startsWith("/api/resident/")) {
            allowed = "RESIDENT".equals(role);
        } else if (path.startsWith("/api/property/")) {
            allowed = "PROPERTY".equals(role) || "ADMIN".equals(role);
        } else if (path.startsWith("/api/sanitation/")) {
            allowed = "SANITATION".equals(role) || "ADMIN".equals(role);
        } else if (path.startsWith("/api/street/")) {
            allowed = "STREET".equals(role) || "ADMIN".equals(role);
        } else if (path.startsWith("/api/admin/")) {
            allowed = "ADMIN".equals(role);
        } else {
            allowed = true;
        }
        if (!allowed) {
            response.sendError(403, "当前账号无权访问该端口");
            return false;
        }
        request.setAttribute(CurrentUser.ATTR_USER,
                new CurrentUser(tokenValue, role, token.getUserId(), token.getResidentId()));
        return true;
    }

    private boolean isPermitted(String path) {
        return PERMIT_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
