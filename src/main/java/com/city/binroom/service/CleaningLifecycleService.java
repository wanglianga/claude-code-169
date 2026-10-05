package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.Complaint;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.CleaningTaskRepository;
import com.city.binroom.domain.repo.ComplaintRepository;
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

/** 保洁/消杀任务生命周期：派单、完成，并联动投诉与垃圾房状态 */
@Service
@RequiredArgsConstructor
public class CleaningLifecycleService {

    private final CleaningTaskRepository cleaningRepo;
    private final ComplaintRepository complaintRepo;
    private final BinRoomRepository roomRepo;
    private final RoomStateService stateService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public CleaningTask assign(Long taskId, String assignee) {
        CleaningTask task = cleaningRepo.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("保洁任务不存在: " + taskId));
        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.DISPATCHED);
        }
        task.setAssignee(assignee);
        task.setAssignedAt(LocalDateTime.now());
        return cleaningRepo.save(task);
    }

    /** 完成保洁/消杀；同来源的未结投诉一并闭环 */
    @Transactional
    public CleaningTask complete(Long taskId, String note) {
        CleaningTask task = cleaningRepo.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("保洁任务不存在: " + taskId));
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(now);
        task.setResultNote(note);
        cleaningRepo.save(task);

        BinRoom room = task.getBinRoom();
        room.setLastCleanedAt(now);
        if ("DISINFECT".equals(task.getTaskKind())) {
            room.setOdorLevel(Math.min(room.getOdorLevel(), 10));
        }
        roomRepo.save(room);

        // 联动闭环：该垃圾房同类型未结投诉自动标记已处理
        for (Complaint c : complaintRepo.findByBinRoomIdOrderByCreatedAtDesc(room.getId())) {
            if (c.getStatus() != TaskStatus.DONE && c.getStatus() != TaskStatus.CANCELLED
                    && ("DISINFECT".equals(task.getTaskKind()) || "POST_PICKUP_CLEAN".equals(task.getTaskKind()))) {
                c.setStatus(TaskStatus.DONE);
                c.setCleaningTask(task);
                c.setHandleNote("由保洁/消杀任务 " + task.getTaskNo() + " 联动闭环：" + note);
                c.setHandledAt(now);
                complaintRepo.save(c);
                stateService.log(room, LinkageType.ODOR_COMPLAINT.name(), "COMPLETE",
                        "投诉 " + c.getComplaintNo() + " 随任务 " + task.getTaskNo() + " 闭环", c.getComplaintNo());
            }
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("taskId", task.getId());
        payload.put("taskNo", task.getTaskNo());
        payload.put("roomId", room.getId());
        payload.put("taskKind", task.getTaskKind());
        payload.put("linkageType", task.getLinkageType() == null ? "" : task.getLinkageType());
        eventPublisher.publish(Topics.LINKAGE_EVENTS, EventTypes.CLEANING_COMPLETED, payload);
        return task;
    }
}
