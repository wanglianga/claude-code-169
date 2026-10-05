package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.LinkageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LinkageLogRepository extends JpaRepository<LinkageLog, Long> {
    List<LinkageLog> findTop100ByBinRoomIdOrderByCreatedAtDesc(Long roomId);

    boolean existsByLinkageTypeAndRefBizNoAndAction(String linkageType, String refBizNo, String action);
}
