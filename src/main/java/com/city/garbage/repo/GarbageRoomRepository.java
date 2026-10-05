package com.city.garbage.repo;

import com.city.garbage.domain.GarbageRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarbageRoomRepository extends JpaRepository<GarbageRoom, Long> {
    Optional<GarbageRoom> findByCode(String code);
    List<GarbageRoom> findByCommunityId(Long communityId);
}
