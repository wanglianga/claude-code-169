package com.city.binroom.web;

import com.city.binroom.domain.entity.Resident;
import com.city.binroom.domain.enums.AccessMethod;
import com.city.binroom.domain.enums.AiVerdict;
import com.city.binroom.domain.enums.ComplaintType;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.ComplaintRepository;
import com.city.binroom.domain.repo.DisposalRecordRepository;
import com.city.binroom.domain.repo.PointsLedgerRepository;
import com.city.binroom.domain.repo.RectificationRepository;
import com.city.binroom.domain.repo.ResidentRepository;
import com.city.binroom.service.ComplaintService;
import com.city.binroom.service.DisposalService;
import com.city.binroom.service.RectificationService;
import com.city.binroom.web.auth.CurrentUser;
import com.city.binroom.web.dto.ViewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "居民端")
@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentRepository residentRepo;
    private final DisposalRecordRepository disposalRepo;
    private final PointsLedgerRepository ledgerRepo;
    private final ComplaintRepository complaintRepo;
    private final RectificationRepository rectificationRepo;
    private final DisposalService disposalService;
    private final ComplaintService complaintService;
    private final RectificationService rectificationService;
    private final ViewMapper viewMapper;

    private Resident currentResident(HttpServletRequest req) {
        CurrentUser cu = (CurrentUser) req.getAttribute(CurrentUser.ATTR_USER);
        return residentRepo.findById(cu.getResidentId()).orElseThrow();
    }

    @Operation(summary = "我的信息与积分余额")
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest req) {
        return viewMapper.resident(currentResident(req));
    }

    @Operation(summary = "刷卡/扫码投放（记录垃圾类型、重量、积分与违规提示）")
    @PostMapping("/disposals")
    public Map<String, Object> dispose(HttpServletRequest req, @RequestBody DisposeRequest body) {
        Resident r = currentResident(req);
        var record = disposalService.create(r.getCardNo(), body.getRoomCode(), body.getWasteType(),
                body.getAccessMethod(), body.getWeightKg(), body.getAiVerdict(),
                Boolean.TRUE.equals(body.getCreditDelayed()));
        return viewMapper.disposal(record);
    }

    @Operation(summary = "我的投放记录（含违规提示与积分到账状态）")
    @GetMapping("/disposals")
    public List<?> myDisposals(HttpServletRequest req) {
        Resident r = currentResident(req);
        return disposalRepo.findByResidentIdOrderByCreatedAtDesc(r.getId())
                .stream().map(viewMapper::disposal).toList();
    }

    @Operation(summary = "我的积分流水")
    @GetMapping("/points")
    public Map<String, Object> points(HttpServletRequest req) {
        Resident r = currentResident(req);
        return Map.of(
                "balance", r.getPointsBalance(),
                "ledger", ledgerRepo.findByResidentIdOrderByCreatedAtDesc(r.getId())
                        .stream().map(viewMapper::ledger).toList());
    }

    @Operation(summary = "对识别为错投的投放发起申诉（摄像头误判）")
    @PostMapping("/appeals")
    public Map<String, Object> appeal(HttpServletRequest req, @RequestBody AppealRequest body) {
        currentResident(req);
        return viewMapper.rectification(rectificationService.appeal(body.getBizNo(), body.getReason()));
    }

    @Operation(summary = "提交投诉（异味/卫生/满溢扰民等）")
    @PostMapping("/complaints")
    public Map<String, Object> complain(HttpServletRequest req, @RequestBody ComplaintRequest body) {
        Resident r = currentResident(req);
        return viewMapper.complaint(
                complaintService.create(r.getId(), body.getRoomCode(), body.getType(), body.getContent()));
    }

    @Operation(summary = "我的投诉与处理结果")
    @GetMapping("/complaints")
    public List<?> myComplaints(HttpServletRequest req) {
        Resident r = currentResident(req);
        return complaintRepo.findAll().stream()
                .filter(c -> c.getResident() != null && c.getResident().getId().equals(r.getId()))
                .map(viewMapper::complaint).toList();
    }

    @Operation(summary = "我的整改/申诉及核查结论")
    @GetMapping("/rectifications")
    public List<?> myRectifications(HttpServletRequest req) {
        Resident r = currentResident(req);
        return rectificationRepo.findAll().stream()
                .filter(x -> x.getDisposal().getResident().getId().equals(r.getId()))
                .map(viewMapper::rectification).toList();
    }

    @Data
    public static class DisposeRequest {
        @NotBlank
        private String roomCode;
        @NotNull
        private WasteType wasteType;
        @NotNull
        private AccessMethod accessMethod;
        @NotNull
        @Positive
        private Double weightKg;
        /** CORRECT / MIS_SORT / SUSPECTED / UNKNOWN，模拟摄像头识别结论 */
        private AiVerdict aiVerdict = AiVerdict.CORRECT;
        /** true 模拟设备断网/积分链路延迟导致积分暂未到账 */
        private Boolean creditDelayed = false;
    }

    @Data
    public static class AppealRequest {
        @NotBlank
        private String bizNo;
        @NotBlank
        private String reason;
    }

    @Data
    public static class ComplaintRequest {
        @NotBlank
        private String roomCode;
        @NotNull
        private ComplaintType type;
        @NotBlank
        private String content;
    }
}
