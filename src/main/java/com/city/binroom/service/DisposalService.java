package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.entity.Resident;
import com.city.binroom.domain.enums.AccessMethod;
import com.city.binroom.domain.enums.AiVerdict;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.DisposalRecordRepository;
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

/**
 * 居民投放：刷卡/扫码 → 记录小区、楼栋、垃圾类型、重量、积分、违规提示。
 * 摄像头识别为错投时暂扣积分并记违规；疑似错投也暂扣待核查。
 */
@Service
@RequiredArgsConstructor
public class DisposalService {

    private final ResidentRepository residentRepo;
    private final BinRoomRepository roomRepo;
    private final DisposalRecordRepository disposalRepo;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public DisposalRecord create(String cardNo, String roomCode, WasteType wasteType, AccessMethod accessMethod,
                                 double weightKg, AiVerdict aiVerdict, boolean creditDelayed) {
        Resident resident = residentRepo.findByCardNo(cardNo)
                .orElseThrow(() -> new EntityNotFoundException("居民卡号不存在: " + cardNo));
        BinRoom room = roomRepo.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("垃圾房不存在: " + roomCode));

        DisposalRecord record = new DisposalRecord();
        record.setBizNo(com.city.binroom.util.BizNo.of("D"));
        record.setResident(resident);
        record.setBinRoom(room);
        record.setWasteType(wasteType);
        record.setAccessMethod(accessMethod);
        record.setWeightKg(weightKg);
        record.setAiVerdict(aiVerdict == null ? AiVerdict.UNKNOWN : aiVerdict);
        record.setCreatedAt(LocalDateTime.now());
        record.setPointsCredited(false);
        record.setPointsWithheld(false);
        record.setPenaltyApplied(false);
        record.setPointsSettled(false);
        record.setCreditDelayed(creditDelayed);
        record.setPointsAnomalyNoticed(false);

        record.setBasePoints(calcBasePoints(wasteType, weightKg));
        record.setPenaltyPoints(aiVerdict == AiVerdict.MIS_SORT ? 20 : 0);

        switch (record.getAiVerdict()) {
            case MIS_SORT -> {
                record.setPointsWithheld(true);
                record.setViolationTip("摄像头识别为错投：本次投放到【" + wasteType.getLabel()
                        + "】，请按分类要求重新投放。积分暂扣 " + record.getBasePoints()
                        + " 分并记违规扣 " + record.getPenaltyPoints()
                        + " 分；如识别有误可在 48 小时内申诉，申诉成立积分返还。");
            }
            case SUSPECTED -> {
                record.setPointsWithheld(true);
                record.setViolationTip("摄像头疑似识别到混投，本次积分 " + record.getBasePoints()
                        + " 分暂扣，待物业核查后到账或通知整改。");
            }
            case CORRECT -> record.setViolationTip("投放正确，预计到账 " + record.getBasePoints() + " 积分，感谢参与垃圾分类。");
            default -> record.setViolationTip("本次投放已记录，预计到账 " + record.getBasePoints() + " 积分。");
        }

        disposalRepo.save(record);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("disposalId", record.getId());
        payload.put("bizNo", record.getBizNo());
        payload.put("roomId", room.getId());
        payload.put("residentId", resident.getId());
        eventPublisher.publish(Topics.POINTS_EVENTS, EventTypes.DISPOSAL_CREATED, payload);
        return record;
    }

    /** 基础积分：厨余 20 分/kg，可回收物 10 分/kg，其余 0 分 */
    private int calcBasePoints(WasteType type, double weightKg) {
        return switch (type) {
            case KITCHEN -> (int) Math.round(weightKg * 20);
            case RECYCLABLE -> (int) Math.round(weightKg * 10);
            default -> 0;
        };
    }
}
