package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
}
