package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.DisposalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DisposalRecordRepository extends JpaRepository<DisposalRecord, Long> {
    Optional<DisposalRecord> findByBizNo(String bizNo);

    List<DisposalRecord> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    long countByBinRoomIdAndCreatedAtAfter(Long roomId, LocalDateTime after);

    long countByBinRoomIdAndCreatedAtBetween(Long roomId, LocalDateTime start, LocalDateTime end);

    List<DisposalRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<DisposalRecord> findByBinRoomCommunityIdAndCreatedAtBetween(Long communityId,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end);

    List<DisposalRecord> findByPointsCreditedFalseAndCreatedAtBefore(LocalDateTime before);
}
