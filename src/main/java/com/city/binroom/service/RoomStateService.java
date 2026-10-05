package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.LinkageLog;
import com.city.binroom.domain.enums.RoomState;
import com.city.binroom.domain.repo.LinkageLogRepository;
import com.city.binroom.domain.repo.PickupTaskRepository;
import com.city.binroom.domain.repo.ComplaintRepository;
import com.city.binroom.config.LinkageProperties;
import com.city.binroom.domain.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 垃圾房综合状态与清运优先级计算。
 * 四端展示都从这里取状态，避免各端各自判断导致不一致。
 */
@Component
@RequiredArgsConstructor
public class RoomStateService {

    private final LinkageProperties props;
    private final LinkageLogRepository linkageLogRepo;
    private final PickupTaskRepository pickupRepo;
    private final ComplaintRepository complaintRepo;

    /** 计算垃圾房当前综合状态 */
    public RoomState stateOf(BinRoom room) {
        if (Boolean.FALSE.equals(room.getOnline())) {
            return RoomState.OFFLINE;
        }
        boolean full = room.getKitchenFillPct() >= props.getFullThresholdPct()
                || room.getOtherFillPct() >= props.getFullThresholdPct();
        boolean odor = room.getOdorLevel() >= props.getOdorThreshold();
        if (full && odor) {
            return RoomState.FULL_ODOR;
        }
        if (full) {
            return RoomState.FULL;
        }
        if (odor) {
            return RoomState.ODOR;
        }
        if (Boolean.TRUE.equals(room.getNightPeak())) {
            return RoomState.NIGHT_PEAK;
        }
        return RoomState.NORMAL;
    }

    /** 清运优先级评分：容量、满溢等待、异味、夜间高峰、投诉、升级次数综合，分越高越优先 */
    @Transactional(readOnly = true)
    public int pickupPriorityScore(BinRoom room, com.city.binroom.domain.enums.WasteType type) {
        int fill = type == com.city.binroom.domain.enums.WasteType.KITCHEN
                ? room.getKitchenFillPct() : room.getOtherFillPct();
        int score = fill;
        if (type == com.city.binroom.domain.enums.WasteType.KITCHEN && room.getKitchenFullSince() != null) {
            long waitMinutes = java.time.Duration.between(room.getKitchenFullSince(), LocalDateTime.now()).toMinutes();
            score += (int) Math.min(waitMinutes, 60);
        }
        score += room.getOdorLevel() / 2;
        if (Boolean.TRUE.equals(room.getNightPeak())) {
            score += 20;
        }
        long recentComplaints = complaintRepo.countByBinRoomIdAndCreatedAtAfter(
                room.getId(), LocalDateTime.now().minusHours(12));
        score += recentComplaints * 15;
        boolean escalated = pickupRepo.findByStatusIn(
                        List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED, TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE))
                .stream()
                .anyMatch(t -> t.getBinRoom().getId().equals(room.getId())
                        && t.getWasteType() == type && Boolean.TRUE.equals(t.getEscalated()));
        if (escalated) {
            score += 40;
        }
        return Math.min(score, 200);
    }

    @Transactional
    public void log(BinRoom room, String linkageType, String action, String detail, String refBizNo) {
        LinkageLog log = new LinkageLog();
        log.setBinRoom(room);
        log.setLinkageType(linkageType);
        log.setAction(action);
        log.setDetail(detail);
        log.setRefBizNo(refBizNo);
        log.setCreatedAt(LocalDateTime.now());
        linkageLogRepo.save(log);
    }
}
