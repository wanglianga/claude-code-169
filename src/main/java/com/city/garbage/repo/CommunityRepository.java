package com.city.garbage.repo;

import com.city.garbage.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByCode(String code);
}
