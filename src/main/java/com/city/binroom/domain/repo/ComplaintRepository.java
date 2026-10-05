package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.Complaint;
import com.city.binroom.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByBinRoomIdOrderByCreatedAtDesc(Long roomId);

    List<Complaint> findByStatus(TaskStatus status);

    List<Complaint> findByBinRoomCommunityIdAndCreatedAtBetween(Long communityId, LocalDateTime start, LocalDateTime end);

    long countByBinRoomIdAndCreatedAtAfter(Long roomId, LocalDateTime after);
}
