package com.city.binroom.service.processor;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.Complaint;
import com.city.binroom.domain.enums.ComplaintType;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.ComplaintRepository;
import com.city.binroom.event.EventTypes;
import com.city.binroom.event.LinkageMessage;
import com.city.binroom.event.LinkageProcessor;
import com.city.binroom.service.GovernanceTaskService;
import com.city.binroom.service.RoomStateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 异味（及卫生/满溢扰民）投诉 → 立即联动物业消杀保洁，投诉与任务互相绑定 */
@Component
@RequiredArgsConstructor
public class ComplaintLinkageProcessor implements LinkageProcessor {

    private final ComplaintRepository complaintRepo;
    private final BinRoomRepository roomRepo;
    private final GovernanceTaskService taskService;
    private final RoomStateService stateService;

    @Override
    public boolean supports(LinkageMessage m) {
        return EventTypes.COMPLAINT_CREATED.equals(m.getType());
    }

    @Override
    @Transactional
    public void handle(LinkageMessage m) {
        JsonNode p = m.getPayload();
        Long complaintId = p.get("complaintId").asLong();
        Complaint complaint = complaintRepo.findById(complaintId).orElseThrow();
        ComplaintType type = complaint.getType();
        if (type != ComplaintType.ODOR && type != ComplaintType.HYGIENE && type != ComplaintType.OVERFLOW) {
            return;
        }
        BinRoom room = roomRepo.findById(complaint.getBinRoom().getId()).orElseThrow();
        CleaningTask task = taskService.createCleaningIfAbsent(room, "DISINFECT",
                "居民投诉(" + type.getLabel() + ")：" + complaint.getContent(),
                LinkageType.ODOR_COMPLAINT.name(), 60);
        complaint.setCleaningTask(task);
        complaintRepo.save(complaint);
        stateService.log(room, LinkageType.ODOR_COMPLAINT.name(), "TRIGGER",
                "投诉 " + complaint.getComplaintNo() + "（" + type.getLabel()
                        + "）联动消杀任务 " + task.getTaskNo(), complaint.getComplaintNo());
    }
}
