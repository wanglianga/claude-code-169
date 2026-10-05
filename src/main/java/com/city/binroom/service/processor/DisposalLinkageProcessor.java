package com.city.binroom.service.processor;

import com.city.binroom.config.LinkageProperties;
import com.city.binroom.domain.entity.*;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.enums.PointsDirection;
import com.city.binroom.domain.enums.RectifyResult;
import com.city.binroom.domain.repo.*;
import com.city.binroom.event.EventTypes;
import com.city.binroom.event.LinkageMessage;
import com.city.binroom.event.LinkageProcessor;
import com.city.binroom.service.GovernanceTaskService;
import com.city.binroom.service.PointsService;
import com.city.binroom.service.RoomStateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 居民投放事件联动：
 * - 正常投放：积分入账（断网/积分链路延迟时挂起，超时巡检补发）
 * - 错投/疑似：生成整改单、暂扣积分、记违规扣分；居民端展示违规提示
 * - 夜间投放高峰：窗口内次数达标 → 物业巡查 + 环卫待命
 */
@Component
@RequiredArgsConstructor
public class DisposalLinkageProcessor implements LinkageProcessor {

    private final DisposalRecordRepository disposalRepo;
    private final RectificationRepository rectificationRepo;
    private final BinRoomRepository roomRepo;
    private final PointsLedgerRepository ledgerRepo;
    private final ResidentRepository residentRepo;
    private final GovernanceTaskService taskService;
    private final PointsService pointsService;
    private final RoomStateService stateService;
    private final LinkageProperties props;

    @Override
    public boolean supports(LinkageMessage m) {
        return EventTypes.DISPOSAL_CREATED.equals(m.getType());
    }

    @Override
    @Transactional
    public void handle(LinkageMessage m) {
        JsonNode p = m.getPayload();
        Long disposalId = p.get("disposalId").asLong();
        DisposalRecord disposal = disposalRepo.findById(disposalId).orElseThrow();
        BinRoom room = disposal.getBinRoom();
        Resident resident = disposal.getResident();
        LocalDateTime now = LocalDateTime.now();

        switch (disposal.getAiVerdict()) {
            case MIS_SORT, SUSPECTED -> {
                // 生成违规整改单（与投放记录一一对应，居民可在此单上申诉）
                Rectification rect = rectificationRepo
                        .findByDisposalIdAndKind(disposalId, "VIOLATION")
                        .orElseGet(() -> {
                            Rectification r = new Rectification();
                            r.setRectifyNo(com.city.binroom.util.BizNo.of("RC"));
                            r.setDisposal(disposal);
                            r.setBinRoom(room);
                            r.setKind("VIOLATION");
                            r.setResult(RectifyResult.PENDING);
                            r.setCreatedAt(now);
                            return rectificationRepo.save(r);
                        });

                // 违规扣分立即生效，申诉成立时随整改结论返还
                if (!Boolean.TRUE.equals(disposal.getPenaltyApplied()) && disposal.getPenaltyPoints() > 0) {
                    resident.setPointsBalance(resident.getPointsBalance() - disposal.getPenaltyPoints());
                    residentRepo.save(resident);
                    PointsLedger penalty = new PointsLedger();
                    penalty.setResident(resident);
                    penalty.setDirection(PointsDirection.PENALTY);
                    penalty.setPoints(-disposal.getPenaltyPoints());
                    penalty.setDisposalBizNo(disposal.getBizNo());
                    penalty.setReason("摄像头识别错投违规扣分，整改单 " + rect.getRectifyNo());
                    penalty.setCreatedAt(now);
                    ledgerRepo.save(penalty);
                    disposal.setPenaltyApplied(true);
                }
                disposal.setPointsWithheld(true);
                disposal.setPointsSettled(false);
                disposalRepo.save(disposal);
                stateService.log(room, LinkageType.MIS_SORT.name(), "TRIGGER",
                        "居民 " + resident.getName() + " 投放 " + disposal.getWasteType().getLabel()
                                + " " + disposal.getWeightKg() + "kg 被识别为"
                                + disposal.getAiVerdict().getLabel() + "，积分暂扣、记违规，整改单 "
                                + rect.getRectifyNo(),
                        disposal.getBizNo());
            }
            default -> {
                // 正常投放：积分链路正常则即时到账；creditDelayed 模拟断网时积分未到账，等巡检补发
                if (!Boolean.TRUE.equals(disposal.getPointsCredited())
                        && !Boolean.TRUE.equals(disposal.getCreditDelayed())) {
                    pointsService.creditForDisposal(disposal, PointsDirection.EARN,
                            disposal.getBasePoints(), "投放 " + disposal.getWasteType().getLabel()
                                    + " " + disposal.getWeightKg() + "kg 积分到账");
                    disposal.setPointsSettled(true);
                    disposalRepo.save(disposal);
                    stateService.log(room, LinkageType.POINTS_MISSING.name(), "CREDIT",
                            "投放 " + disposal.getBizNo() + " 积分 " + disposal.getBasePoints() + " 已到账",
                            disposal.getBizNo());
                } else if (Boolean.TRUE.equals(disposal.getCreditDelayed())) {
                    stateService.log(room, LinkageType.POINTS_MISSING.name(), "TRIGGER",
                            "投放 " + disposal.getBizNo() + " 时设备/积分链路延迟，积分暂未到账，等待巡检补发",
                            disposal.getBizNo());
                }
            }
        }

        detectNightPeak(room, now);
    }

    private void detectNightPeak(BinRoom room, LocalDateTime now) {
        int h = now.getHour();
        boolean night = props.getNightStartHour() > props.getNightEndHour()
                ? h >= props.getNightStartHour() || h < props.getNightEndHour()
                : h >= props.getNightStartHour() && h < props.getNightEndHour();
        if (!night) {
            return;
        }
        long count = disposalRepo.countByBinRoomIdAndCreatedAtAfter(
                room.getId(), now.minusMinutes(props.getNightPeakWindowMinutes()));
        if (count >= props.getNightPeakCount() && !Boolean.TRUE.equals(room.getNightPeak())) {
            room.setNightPeak(true);
            roomRepo.save(room);
            CleaningTask patrol = taskService.createCleaningIfAbsent(room, "NIGHT_PATROL",
                    props.getNightPeakWindowMinutes() + " 分钟内夜间投放 " + count
                            + " 次，现场引导分类并保洁",
                    LinkageType.NIGHT_PEAK.name(), props.getCleaningTimeoutMinutes());
            PickupTask standby = taskService.createPickupIfAbsent(room,
                    com.city.binroom.domain.enums.WasteType.KITCHEN,
                    "夜间投放高峰，提前安排厨余清运，容量 " + room.getKitchenFillPct() + "%",
                    LinkageType.NIGHT_PEAK.name(), props.getPickupTimeoutMinutes());
            stateService.log(room, LinkageType.NIGHT_PEAK.name(), "TRIGGER",
                    "夜间高峰触发：巡查任务 " + patrol.getTaskNo() + "，清运待命 " + standby.getTaskNo(),
                    patrol.getTaskNo());
        }
    }
}
