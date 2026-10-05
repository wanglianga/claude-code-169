package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.PointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {
    List<PointsLedger> findByResidentIdOrderByCreatedAtDesc(Long residentId);
}
