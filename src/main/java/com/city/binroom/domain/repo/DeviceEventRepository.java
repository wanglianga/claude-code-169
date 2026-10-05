package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.DeviceEvent;
import com.city.binroom.domain.enums.DeviceEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

    long countByBinRoomIdAndEventTypeAndEventTimeAfter(
            Long roomId, DeviceEventType type, LocalDateTime after);

    @Query("select coalesce(sum(e.numericValue),0) from DeviceEvent e " +
            "where e.binRoom.id = :roomId and e.eventType = :type and e.eventTime >= :after")
    double sumNumeric(@Param("roomId") Long roomId,
                      @Param("type") DeviceEventType type,
                      @Param("after") LocalDateTime after);

    List<DeviceEvent> findTop50ByBinRoomIdOrderByEventTimeDesc(Long roomId);
}
