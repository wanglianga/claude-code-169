package com.city.garbage.repo;

import com.city.garbage.domain.PointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {
    List<PointsLedger> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    boolean existsByDisposalIdAndChangePointsGreaterThan(Long disposalId, int min);
}
