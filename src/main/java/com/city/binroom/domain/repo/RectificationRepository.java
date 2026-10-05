package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.Rectification;
import com.city.binroom.domain.enums.RectifyResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RectificationRepository extends JpaRepository<Rectification, Long> {
    Optional<Rectification> findByRectifyNo(String rectifyNo);

    List<Rectification> findByResult(RectifyResult result);

    Optional<Rectification> findByDisposalIdAndKind(Long disposalId, String kind);
}
