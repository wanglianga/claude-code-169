package com.city.binroom.service;

import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.entity.PointsLedger;
import com.city.binroom.domain.entity.Resident;
import com.city.binroom.domain.enums.PointsDirection;
import com.city.binroom.domain.repo.DisposalRecordRepository;
import com.city.binroom.domain.repo.PointsLedgerRepository;
import com.city.binroom.domain.repo.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 积分入账：每笔变动写流水并更新余额，与投放单号严格对应 */
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsLedgerRepository ledgerRepo;
    private final ResidentRepository residentRepo;
    private final DisposalRecordRepository disposalRepo;

    @Transactional
    public void creditForDisposal(DisposalRecord disposal, PointsDirection direction, int points, String reason) {
        Resident resident = disposal.getResident();
        resident.setPointsBalance(resident.getPointsBalance() + points);
        residentRepo.save(resident);

        PointsLedger ledger = new PointsLedger();
        ledger.setResident(resident);
        ledger.setDirection(direction);
        ledger.setPoints(points);
        ledger.setDisposalBizNo(disposal.getBizNo());
        ledger.setReason(reason);
        ledger.setCreatedAt(LocalDateTime.now());
        ledgerRepo.save(ledger);

        disposal.setPointsCredited(true);
        disposalRepo.save(disposal);
    }
}
