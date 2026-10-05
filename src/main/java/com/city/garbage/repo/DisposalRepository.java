package com.city.garbage.repo;

import com.city.garbage.domain.DisposalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface DisposalRepository extends JpaRepository<DisposalRecord, Long> {
    List<DisposalRecord> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<DisposalRecord> findByRoomCodeOrderByCreatedAtDesc(String roomCode);
    List<DisposalRecord> findByCommunityIdAndCreatedAtBetween(Long communityId, OffsetDateTime from, OffsetDateTime to);
    List<DisposalRecord> findByStatusAndCreatedAtBefore(com.city.garbage.domain.DisposalStatus status, OffsetDateTime before);
}
