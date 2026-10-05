package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.PickupTaskRepository;
import com.city.binroom.config.LinkageProperties;
import com.city.binroom.event.EventPublisher;
import com.city.binroom.event.EventTypes;
import com.city.binroom.event.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 清运任务生命周期：派单、清运车到场、清运完成并联动“清运后清洁” */
@Service
@RequiredArgsConstructor
public class PickupLifecycleService {

    private final PickupTaskRepository pickupRepo;
    private final BinRoomRepository roomRepo;
    private final GovernanceTaskService taskService;
    private final RoomStateService stateService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final LinkageProperties props;

    @Transactional
    public PickupTask assign(Long taskId, String assignee, String routeCode) {
        PickupTask task = mustGet(taskId);
        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.DISPATCHED);
        }
        task.setAssignee(assignee);
        task.setRouteCode(routeCode);
        task.setAssignedAt(LocalDateTime.now());
        return pickupRepo.save(task);
    }

    /** 清运车到场：满溢等待计时解除，任务进入处理中 */
    @Transactional
    public PickupTask arrive(Long taskId) {
        PickupTask task = mustGet(taskId);
        BinRoom room = task.getBinRoom();
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setArrivedAt(now);
        pickupRepo.save(task);
        room.setTruckOnSite(true);
        roomRepo.save(room);
        stateService.log(room, task.getLinkageType() == null
                        ? LinkageType.FULL_NO_TRUCK.name() : task.getLinkageType(),
                "COMPLETE", "清运车到场，任务 " + task.getTaskNo() + " 开始清运，满溢等待解除", task.getTaskNo());
        return task;
    }

    /**
     * 清运完成：容量清零、满溢计时清除，并自动生成“清运后清洁”任务；
     * 清洁未在时限内完成由定时巡检升级（清运后未清洁场景）。
     */
    @Transactional
    public PickupTask complete(Long taskId, Double pickedWeightKg, String note) {
        PickupTask task = mustGet(taskId);
        BinRoom room = task.getBinRoom();
        LocalDateTime now = LocalDateTime.now();

        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(now);
        task.setPickedWeightKg(pickedWeightKg);
        task.setResultNote(note);
        pickupRepo.save(task);

        if (task.getWasteType() == WasteType.KITCHEN) {
            room.setKitchenFillPct(10);
            room.setKitchenFullSince(null);
        } else if (task.getWasteType() == WasteType.OTHER) {
            room.setOtherFillPct(10);
        }
        room.setTruckOnSite(false);
        room.setLastPickupAt(now);
        roomRepo.save(room);

        CleaningTask cleanTask = taskService.createCleaningIfAbsent(room,
                "POST_PICKUP_CLEAN",
                "清运任务 " + task.getTaskNo() + " 完成后需清洁消杀（" + task.getWasteType().getLabel() + "）",
                LinkageType.NOT_CLEANED_AFTER_PICKUP.name(),
                task.getTaskNo(),
                props.getPostPickupCleanMinutes());
        stateService.log(room, LinkageType.NOT_CLEANED_AFTER_PICKUP.name(), "TRIGGER",
                "清运 " + task.getTaskNo() + " 完成，生成清运后清洁任务 " + cleanTask.getTaskNo(), task.getTaskNo());

        taskService.recalcPriorities();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("taskId", task.getId());
        payload.put("taskNo", task.getTaskNo());
        payload.put("roomId", room.getId());
        payload.put("wasteType", task.getWasteType().name());
        payload.put("cleanTaskNo", cleanTask.getTaskNo());
        eventPublisher.publish(Topics.LINKAGE_EVENTS, EventTypes.PICKUP_COMPLETED, payload);
        return task;
    }

    private PickupTask mustGet(Long id) {
        return pickupRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("清运任务不存在: " + id));
    }
}
