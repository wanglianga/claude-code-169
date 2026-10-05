package com.city.binroom.service;

import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.entity.Rectification;
import com.city.binroom.domain.enums.RectifyResult;
import com.city.binroom.domain.repo.DisposalRecordRepository;
import com.city.binroom.domain.repo.RectificationRepository;
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
 * 整改与申诉：
 * - 居民对错投识别发起申诉（摄像头误判场景）
 * - 物业核查违规整改 / 申诉，结论事件驱动积分返还或没收
 */
@Service
@RequiredArgsConstructor
public class RectificationService {

    private final RectificationRepository rectificationRepo;
    private final DisposalRecordRepository disposalRepo;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public Rectification appeal(String disposalBizNo, String reason) {
        DisposalRecord disposal = disposalRepo.findByBizNo(disposalBizNo)
                .orElseThrow(() -> new EntityNotFoundException("投放记录不存在: " + disposalBizNo));
        Rectification rect = rectificationRepo
                .findByDisposalIdAndKind(disposal.getId(), "APPEAL")
                .orElseGet(() -> {
                    Rectification r = new Rectification();
                    r.setRectifyNo(com.city.binroom.util.BizNo.of("AP"));
                    r.setDisposal(disposal);
                    r.setBinRoom(disposal.getBinRoom());
                    r.setKind("APPEAL");
                    r.setResult(RectifyResult.PENDING);
                    r.setCreatedAt(LocalDateTime.now());
                    return rectificationRepo.save(r);
                });
        rect.setAppealReason(reason);
        rect.setResult(RectifyResult.PENDING);
        rect.setReviewedAt(null);
        rectificationRepo.save(rect);
        return rect;
    }

    @Transactional
    public Rectification review(String rectifyNo, RectifyResult result, String note, String reviewer) {
        Rectification rect = rectificationRepo.findByRectifyNo(rectifyNo)
                .orElseThrow(() -> new EntityNotFoundException("整改单不存在: " + rectifyNo));
        rect.setResult(result);
        rect.setReviewNote(note);
        rect.setReviewer(reviewer);
        rect.setReviewedAt(LocalDateTime.now());
        rectificationRepo.save(rect);
        publishReviewed(rect);
        return rect;
    }

    private void publishReviewed(Rectification rect) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("rectificationId", rect.getId());
        payload.put("rectifyNo", rect.getRectifyNo());
        payload.put("disposalId", rect.getDisposal().getId());
        payload.put("kind", rect.getKind());
        payload.put("result", rect.getResult().name());
        eventPublisher.publish(Topics.LINKAGE_EVENTS, EventTypes.RECTIFICATION_REVIEWED, payload);
    }
}
