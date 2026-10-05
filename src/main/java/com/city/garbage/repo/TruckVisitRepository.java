package com.city.garbage.repo;

import com.city.garbage.domain.TruckVisit;
import com.city.garbage.domain.TruckVisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TruckVisitRepository extends JpaRepository<TruckVisit, Long> {
    Optional<TruckVisit> findFirstByRoomCodeAndStatusInOrderByArrivedAtDesc(String roomCode, List<TruckVisitStatus> statuses);
    List<TruckVisit> findByRoomCodeOrderByArrivedAtDesc(String roomCode);
}
