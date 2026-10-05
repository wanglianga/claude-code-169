package com.city.binroom.web.auth;

import com.city.binroom.domain.entity.AuthToken;
import com.city.binroom.domain.entity.Resident;
import com.city.binroom.domain.entity.SysUser;
import com.city.binroom.domain.repo.AuthTokenRepository;
import com.city.binroom.domain.repo.ResidentRepository;
import com.city.binroom.domain.repo.SysUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository sysUserRepo;
    private final ResidentRepository residentRepo;
    private final AuthTokenRepository tokenRepo;

    @Transactional
    public Map<String, Object> staffLogin(String username, String password) {
        SysUser user = sysUserRepo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("账号不存在"));
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("密码错误");
        }
        AuthToken token = issue(user.getRole(), user.getId(), null);
        return Map.of(
                "token", token.getToken(),
                "role", user.getRole(),
                "displayName", user.getDisplayName(),
                "username", user.getUsername(),
                "communityId", user.getCommunityId() == null ? "" : user.getCommunityId());
    }

    /** 居民凭卡号（刷卡/扫码场景的身份标识）登录 */
    @Transactional
    public Map<String, Object> residentLogin(String cardNo) {
        Resident resident = residentRepo.findByCardNo(cardNo)
                .orElseThrow(() -> new EntityNotFoundException("卡号不存在: " + cardNo));
        AuthToken token = issue("RESIDENT", null, resident.getId());
        return Map.of(
                "token", token.getToken(),
                "role", "RESIDENT",
                "displayName", resident.getName(),
                "cardNo", resident.getCardNo(),
                "residentId", resident.getId());
    }

    private AuthToken issue(String role, Long userId, Long residentId) {
        AuthToken t = new AuthToken();
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setRole(role);
        t.setUserId(userId);
        t.setResidentId(residentId);
        t.setCreatedAt(LocalDateTime.now());
        return tokenRepo.save(t);
    }
}
