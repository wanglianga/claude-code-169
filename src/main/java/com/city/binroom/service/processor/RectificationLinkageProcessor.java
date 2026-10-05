package com.city.binroom.service.processor;

import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.entity.PointsLedger;
import com.city.binroom.domain.entity.Rectification;
import com.city.binroom.domain.entity.Resident;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.enums.PointsDirection;
import com.city.binroom.domain.enums.RectifyResult;
import com.city.binroom.domain.repo.DisposalRecordRepository;
import com.city.binroom.domain.repo.PointsLedgerRepository;
import com.city.binroom.domain.repo.RectificationRepository;
import com.city.binroom.domain.repo.ResidentRepository;
import com.city.binroom.event.EventTypes;
import com.city.binroom.event.LinkageMessage;
import com.city.binroom.event.LinkageProcessor;
import com.city.binroom.service.RoomStateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 整改/申诉核查结论联动（摄像头误判场景）：
 * - PASS / WAIVED：误判撤销，返还扣分、补发暂扣积分，投放违规解除
 * - FAIL：整改不合格，暂扣积分没收并留违规记录
 * 积分流水与投放单号一一对应，居民端余额随之更新。
 */
@Component
@RequiredArgsConstructor
public class RectificationLinkageProcessor implements LinkageProcessor {

    private final RectificationRepository rectificationRepo;
    private final DisposalRecordRepository disposalRepo;
    private final ResidentRepository residentRepo;
    private final PointsLedgerRepository ledgerRepo;
    private final RoomStateService stateService;

    @Override
    public boolean supports(LinkageMessage m) {
        return EventTypes.RECTIFICATION_REVIEWED.equals(m.getType());
    }

    @Override
    @Transactional
    public void handle(LinkageMessage m) {
        JsonNode p = m.getPayload();
        Long rectId = p.get("rectificationId").asLong();
        Rectification rect = rectificationRepo.findById(rectId).orElseThrow();
        if (rect.getResult() == RectifyResult.PENDING) {
            return;
        }
        DisposalRecord disposal = rect.getDisposal();
        Resident resident = disposal.getResident();
        LocalDateTime now = LocalDateTime.now();

        if (rect.getResult() == RectifyResult.PASS || rect.getResult() == RectifyResult.WAIVED) {
            // 返还违规扣分
            if (Boolean.TRUE.equals(disposal.getPenaltyApplied()) && disposal.getPenaltyPoints() > 0) {
                resident.setPointsBalance(resident.getPointsBalance() + disposal.getPenaltyPoints());
                ledger(resident, PointsDirection.MAKEUP, disposal.getPenaltyPoints(), disposal.getBizNo(),
                        "整改/申诉通过（" + rect.getRectifyNo() + "），返还违规扣分");
                disposal.setPenaltyApplied(false);
            }
            // 补发暂扣的投放积分
            if (Boolean.TRUE.equals(disposal.getPointsWithheld())
                    && !Boolean.TRUE.equals(disposal.getPointsCredited())
                    && disposal.getBasePoints() > 0) {
                resident.setPointsBalance(resident.getPointsBalance() + disposal.getBasePoints());
                ledger(resident, PointsDirection.MAKEUP, disposal.getBasePoints(), disposal.getBizNo(),
                        "摄像头误判撤销（" + rect.getRectifyNo() + "），补发暂扣积分");
                disposal.setPointsCredited(true);
            }
            disposal.setPointsWithheld(false);
            disposal.setPointsSettled(true);
            disposal.setViolationTip("整改/申诉已通过，违规记录撤销，积分已按核查结论返还。");
            residentRepo.save(resident);
            disposalRepo.save(disposal);
            stateService.log(disposal.getBinRoom(), LinkageType.AI_APPEAL.name(), "REVERSE",
                    "投放 " + disposal.getBizNo() + " 核查结论 " + rect.getResult().getLabel()
                            + "，违规撤销、积分返还", disposal.getBizNo());
        } else if (rect.getResult() == RectifyResult.FAIL) {
            if (Boolean.TRUE.equals(disposal.getPointsWithheld())) {
                disposal.setPointsCredited(false);
            }
            disposal.setPointsSettled(true);
            disposal.setViolationTip("整改核查不合格，暂扣积分不予发放，请按规范分类投放。");
            disposalRepo.save(disposal);
            stateService.log(disposal.getBinRoom(), LinkageType.MIS_SORT.name(), "COMPLETE",
                    "投放 " + disposal.getBizNo() + " 整改不合格，暂扣积分没收", disposal.getBizNo());
        }
    }

    private void ledger(Resident resident, PointsDirection direction, int points,
                        String bizNo, String reason) {
        PointsLedger l = new PointsLedger();
        l.setResident(resident);
        l.setDirection(direction);
        l.setPoints(points);
        l.setDisposalBizNo(bizNo);
        l.setReason(reason);
        l.setCreatedAt(LocalDateTime.now());
        ledgerRepo.save(l);
    }
}
