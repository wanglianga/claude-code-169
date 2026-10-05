package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {
}
