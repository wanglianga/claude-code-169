package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    List<Building> findByCommunityId(Long communityId);
}
