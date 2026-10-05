package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.BinRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BinRoomRepository extends JpaRepository<BinRoom, Long> {
    Optional<BinRoom> findByCode(String code);

    List<BinRoom> findByCommunityId(Long communityId);
}
