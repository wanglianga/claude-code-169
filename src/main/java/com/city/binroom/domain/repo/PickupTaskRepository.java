package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupTaskRepository extends JpaRepository<PickupTask, Long> {

    List<PickupTask> findByStatusIn(List<TaskStatus> statuses);

    List<PickupTask> findByBinRoomIdAndWasteTypeAndStatusIn(Long roomId,
                                                            com.city.binroom.domain.enums.WasteType wasteType,
                                                            List<TaskStatus> statuses);

    List<PickupTask> findByStatusAndExpectedBeforeBefore(TaskStatus status, java.time.LocalDateTime now);
}
