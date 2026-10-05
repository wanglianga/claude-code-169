package com.city.binroom.service;

import com.city.binroom.config.LinkageProperties;
import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.enums.PointsDirection;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.CleaningTaskRepository;
import com.city.binroom.domain.repo.DisposalRecordRepository;
import com.city.binroom.domain.repo.PickupTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时联动巡检（也可由管理接口手动触发，便于演示与测试）：
 * 1. 设备断网检测 → 派物业维修任务
 * 2. 积分未到账超时 → 自动补发并留痕
 * 3. 厨余满溢但清运车超时未到 → 升级街道/环卫加急
 * 4. 清运后未按时清洁 → 升级督办
 * 5. 保洁/清运任务超时 → 超时/升级状态
 * 6. 夜间高峰解除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkageSweepService {

    private static final List<TaskStatus> OPEN = List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED,
            TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE);

    private final BinRoomRepository roomRepo;
    private final CleaningTaskRepository cleaningRepo;
    private final PickupTaskRepository pickupRepo;
    private final DisposalRecordRepository disposalRepo;
    private final GovernanceTaskService taskService;
    private final PointsService pointsService;
    private final RoomStateService stateService;
    private final LinkageProperties props;

    @Transactional
    public String runSweep() {
        LocalDateTime now = LocalDateTime.now();
        int actions = 0;
        for (BinRoom room : roomRepo.findAll()) {
            actions += detectOffline(room, now);
            actions += escalateFullNoTruck(room, now);
            actions += clearNightPeak(room, now);
        }
        actions += creditMissingPoints(now);
        actions += checkCleaningOverdue(now);
        actions += checkPickupOverdue(now);
        actions += checkPostPickupClean(now);
        taskService.recalcPriorities();
        return "巡检完成，联动动作 " + actions + " 项";
    }

    private int detectOffline(BinRoom room, LocalDateTime now) {
        if (Boolean.TRUE.equals(room.getOnline())) {
            if (room.getLastHeartbeatAt() != null
                    && room.getLastHeartbeatAt().isBefore(now.minusMinutes(props.getOfflineMinutes()))) {
                room.setOnline(false);
                roomRepo.save(room);
                CleaningTask repair = taskService.createCleaningIfAbsent(room, "DEVICE_REPAIR",
                        "设备超过 " + props.getOfflineMinutes() + " 分钟无心跳，疑似断网，需现场检修网络/设备",
                        LinkageType.DEVICE_OFFLINE.name(), props.getCleaningTimeoutMinutes());
                stateService.log(room, LinkageType.DEVICE_OFFLINE.name(), "TRIGGER",
                        "设备断网，已派维修任务 " + repair.getTaskNo() + "；期间投放按线下登记补发积分",
                        repair.getTaskNo());
                return 1;
            }
        }
        return 0;
    }

    private int escalateFullNoTruck(BinRoom room, LocalDateTime now) {
        if (room.getKitchenFullSince() == null || Boolean.TRUE.equals(room.getTruckOnSite())) {
            return 0;
        }
        if (room.getKitchenFillPct() < props.getFullThresholdPct()) {
            return 0;
        }
        long wait = java.time.Duration.between(room.getKitchenFullSince(), now).toMinutes();
        if (wait < props.getFullEscalateMinutes()) {
            return 0;
        }
        return taskService.findOpenPickup(room.getId(), WasteType.KITCHEN).map(task -> {
            if (task.getStatus() != TaskStatus.ESCALATED) {
                task.setStatus(TaskStatus.ESCALATED);
                task.setEscalated(true);
                pickupRepo.save(task);
                stateService.log(room, LinkageType.FULL_NO_TRUCK.name(), "ESCALATE",
                        "厨余满溢已等待 " + wait + " 分钟清运车仍未到，清运任务 "
                                + task.getTaskNo() + " 升级为街道/环卫加急", task.getTaskNo());
                return 1;
            }
            return 0;
        }).orElse(0);
    }

    private int creditMissingPoints(LocalDateTime now) {
        int n = 0;
        List<DisposalRecord> stale = disposalRepo.findByPointsCreditedFalseAndCreatedAtBefore(
                now.minusSeconds(props.getPointsCreditTimeoutSeconds()));
        for (DisposalRecord d : stale) {
            // 暂扣（错投待核查）的不属于“积分未到账”
            if (Boolean.TRUE.equals(d.getPointsWithheld())) {
                continue;
            }
            if (!Boolean.TRUE.equals(d.getPointsAnomalyNoticed())) {
                d.setPointsAnomalyNoticed(true);
                disposalRepo.save(d);
                stateService.log(d.getBinRoom(), LinkageType.POINTS_MISSING.name(), "ESCALATE",
                        "投放 " + d.getBizNo() + " 积分超过 "
                                + props.getPointsCreditTimeoutSeconds() + " 秒未到账，触发自动补发", d.getBizNo());
            }
            pointsService.creditForDisposal(d, PointsDirection.MAKEUP, d.getBasePoints(),
                    "积分未到账巡检补发（投放单 " + d.getBizNo() + "）");
            d.setPointsSettled(true);
            disposalRepo.save(d);
            n++;
        }
        return n;
    }

    private int checkPostPickupClean(LocalDateTime now) {
        int n = 0;
        for (CleaningTask t : cleaningRepo.findByStatusInOrderByCreatedAtAsc(OPEN)) {
            if ("POST_PICKUP_CLEAN".equals(t.getTaskKind())
                    && t.getExpectedBefore() != null && t.getExpectedBefore().isBefore(now)
                    && !Boolean.TRUE.equals(t.getEscalated())) {
                t.setStatus(TaskStatus.ESCALATED);
                t.setEscalated(true);
                cleaningRepo.save(t);
                stateService.log(t.getBinRoom(), LinkageType.NOT_CLEANED_AFTER_PICKUP.name(), "ESCALATE",
                        "清运任务 " + t.getSourceTaskNo() + " 后超过 "
                                + props.getPostPickupCleanMinutes() + " 分钟未清洁，清洁任务 "
                                + t.getTaskNo() + " 升级物业督办", t.getTaskNo());
                n++;
            }
        }
        return n;
    }

    private int checkCleaningOverdue(LocalDateTime now) {
        int n = 0;
        for (CleaningTask t : cleaningRepo.findByStatusInOrderByCreatedAtAsc(
                List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED))) {
            if (t.getExpectedBefore() != null && t.getExpectedBefore().isBefore(now)
                    && !"POST_PICKUP_CLEAN".equals(t.getTaskKind())) {
                t.setStatus(TaskStatus.OVERDUE);
                cleaningRepo.save(t);
                stateService.log(t.getBinRoom(),
                        t.getLinkageType() == null ? "CLEANING" : t.getLinkageType(),
                        "ESCALATE", "保洁/消杀任务 " + t.getTaskNo() + " 已超时，物业端标记超时", t.getTaskNo());
                n++;
            }
        }
        return n;
    }

    private int checkPickupOverdue(LocalDateTime now) {
        int n = 0;
        for (PickupTask t : pickupRepo.findByStatusIn(List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED))) {
            if (t.getExpectedBefore() != null && t.getExpectedBefore().isBefore(now)) {
                t.setStatus(TaskStatus.OVERDUE);
                t.setEscalated(true);
                pickupRepo.save(t);
                stateService.log(t.getBinRoom(), LinkageType.FULL_NO_TRUCK.name(), "ESCALATE",
                        "清运任务 " + t.getTaskNo() + " 超过承诺时限，升级环卫加急", t.getTaskNo());
                n++;
            }
        }
        return n;
    }

    private int clearNightPeak(BinRoom room, LocalDateTime now) {
        if (!Boolean.TRUE.equals(room.getNightPeak())) {
            return 0;
        }
        int h = now.getHour();
        boolean night = props.getNightStartHour() > props.getNightEndHour()
                ? h >= props.getNightStartHour() || h < props.getNightEndHour()
                : h >= props.getNightStartHour() && h < props.getNightEndHour();
        long recent = disposalRepo.countByBinRoomIdAndCreatedAtAfter(
                room.getId(), now.minusMinutes(props.getNightPeakWindowMinutes()));
        if (!night || recent == 0) {
            room.setNightPeak(false);
            roomRepo.save(room);
            stateService.log(room, LinkageType.NIGHT_PEAK.name(), "COMPLETE",
                    "夜间投放高峰解除，现场恢复正常", null);
            return 1;
        }
        return 0;
    }
}
