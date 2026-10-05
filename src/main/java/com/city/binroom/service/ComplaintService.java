package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.Complaint;
import com.city.binroom.domain.entity.Resident;
import com.city.binroom.domain.enums.ComplaintType;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.ComplaintRepository;
import com.city.binroom.domain.repo.ResidentRepository;
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

/** 居民投诉接入：只落投诉事实并发布联动事件，消杀派单由 ComplaintLinkageProcessor 完成 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepo;
    private final BinRoomRepository roomRepo;
    private final ResidentRepository residentRepo;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public Complaint create(Long residentId, String roomCode, ComplaintType type, String content) {
        BinRoom room = roomRepo.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("垃圾房不存在: " + roomCode));
        Resident resident = residentId == null ? null
                : residentRepo.findById(residentId).orElse(null);

        Complaint complaint = new Complaint();
        complaint.setComplaintNo(com.city.binroom.util.BizNo.of("TS"));
        complaint.setResident(resident);
        complaint.setBinRoom(room);
        complaint.setType(type);
        complaint.setContent(content);
        complaint.setStatus(TaskStatus.PENDING);
        complaint.setCreatedAt(LocalDateTime.now());
        complaintRepo.save(complaint);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("complaintId", complaint.getId());
        payload.put("complaintNo", complaint.getComplaintNo());
        payload.put("roomId", room.getId());
        payload.put("type", type.name());
        eventPublisher.publish(Topics.LINKAGE_EVENTS, EventTypes.COMPLAINT_CREATED, payload);
        return complaint;
    }
}
