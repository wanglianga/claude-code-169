package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.CleaningTaskRepository;
import com.city.binroom.domain.repo.PickupTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 治理任务（保洁/消杀/清运）创建与查询，统一幂等入口 */
@Service
@RequiredArgsConstructor
public class GovernanceTaskService {

    private static final List<TaskStatus> OPEN_STATUSES =
            List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED, TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE, TaskStatus.ESCALATED);

    private final CleaningTaskRepository cleaningRepo;
    private final PickupTaskRepository pickupRepo;
    private final RoomStateService stateService;

    @Transactional(readOnly = true)
    public Optional<CleaningTask> findOpenCleaning(Long roomId, String kind) {
        return cleaningRepo.findByBinRoomIdAndTaskKindAndStatusIn(roomId, kind, OPEN_STATUSES).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<PickupTask> findOpenPickup(Long roomId, WasteType type) {
        return pickupRepo.findByBinRoomIdAndWasteTypeAndStatusIn(roomId, type, OPEN_STATUSES).stream().findFirst();
    }

    /** 幂等创建保洁/消杀任务；同垃圾房同类任务未关闭时直接复用 */
    @Transactional
    public CleaningTask createCleaningIfAbsent(BinRoom room, String kind, String reason,
                                               String linkageType, int expectedInMinutes) {
        return createCleaningIfAbsent(room, kind, reason, linkageType, null, expectedInMinutes);
    }

    @Transactional
    public CleaningTask createCleaningIfAbsent(BinRoom room, String kind, String reason,
                                               String linkageType, String sourceTaskNo,
                                               int expectedInMinutes) {
        Optional<CleaningTask> existing = findOpenCleaning(room.getId(), kind);
        if (existing.isPresent()) {
            return existing.get();
        }
        CleaningTask task = new CleaningTask();
        task.setTaskNo(com.city.binroom.util.BizNo.of("CL"));
        task.setBinRoom(room);
        task.setTaskKind(kind);
        task.setReason(reason);
        task.setLinkageType(linkageType);
        task.setSourceTaskNo(sourceTaskNo);
        task.setStatus(TaskStatus.PENDING);
        task.setExpectedBefore(LocalDateTime.now().plusMinutes(expectedInMinutes));
        task.setCreatedAt(LocalDateTime.now());
        task.setEscalated(false);
        return cleaningRepo.save(task);
    }

    /** 幂等创建清运任务 */
    @Transactional
    public PickupTask createPickupIfAbsent(BinRoom room, WasteType type, String reason,
                                           String linkageType, int expectedInMinutes) {
        Optional<PickupTask> existing = findOpenPickup(room.getId(), type);
        if (existing.isPresent()) {
            return existing.get();
        }
        PickupTask task = new PickupTask();
        task.setTaskNo(com.city.binroom.util.BizNo.of("PK"));
        task.setBinRoom(room);
        task.setWasteType(type);
        task.setReason(reason);
        task.setLinkageType(linkageType);
        task.setStatus(TaskStatus.PENDING);
        task.setExpectedBefore(LocalDateTime.now().plusMinutes(expectedInMinutes));
        task.setCreatedAt(LocalDateTime.now());
        task.setEscalated(false);
        task.setCleanedAfterPickup(false);
        task.setPriorityScore(stateService.pickupPriorityScore(room, type));
        return pickupRepo.save(task);
    }

    /** 重算所有未完成清运任务的优先级评分（环卫端排序实时反映容量/异味/投诉变化） */
    @Transactional
    public void recalcPriorities() {
        for (PickupTask task : pickupRepo.findByStatusIn(OPEN_STATUSES)) {
            task.setPriorityScore(stateService.pickupPriorityScore(task.getBinRoom(), task.getWasteType()));
            pickupRepo.save(task);
        }
    }
}
