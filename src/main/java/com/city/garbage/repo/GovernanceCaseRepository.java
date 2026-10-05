package com.city.garbage.repo;

import com.city.garbage.domain.CaseStatus;
import com.city.garbage.domain.GovernanceCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GovernanceCaseRepository extends JpaRepository<GovernanceCase, Long> {
    Optional<GovernanceCase> findByCaseNo(String caseNo);
    List<GovernanceCase> findByRoomCodeAndStatusNot(String roomCode, CaseStatus status);
    List<GovernanceCase> findByStatusIn(List<CaseStatus> statuses);
}
