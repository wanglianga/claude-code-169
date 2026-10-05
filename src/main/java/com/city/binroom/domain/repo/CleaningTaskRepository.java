package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    List<CleaningTask> findByBinRoomIdAndStatusInOrderByCreatedAtDesc(Long roomId, List<TaskStatus> statuses);

    List<CleaningTask> findByStatusInOrderByCreatedAtAsc(List<TaskStatus> statuses);

    List<CleaningTask> findByBinRoomIdAndTaskKindAndStatusIn(Long roomId, String kind, List<TaskStatus> statuses);

    List<CleaningTask> findByStatusAndExpectedBeforeBefore(TaskStatus status, java.time.LocalDateTime now);
}
