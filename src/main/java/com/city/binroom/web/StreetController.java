package com.city.binroom.web;

import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.LinkageLogRepository;
import com.city.binroom.service.StatsService;
import com.city.binroom.web.dto.ViewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "街道端")
@RestController
@RequestMapping("/api/street")
@RequiredArgsConstructor
public class StreetController {

    private final StatsService statsService;
    private final BinRoomRepository roomRepo;
    private final LinkageLogRepository linkageLogRepo;
    private final ViewMapper viewMapper;

    @Operation(summary = "按小区查看分类质量（正确/错投/疑似、重量、积分、正确率）")
    @GetMapping("/classification-quality")
    public List<?> quality(@RequestParam(defaultValue = "7") int days) {
        return statsService.classificationQuality(days);
    }

    @Operation(summary = "投诉趋势（按日期与投诉类型）")
    @GetMapping("/complaint-trend")
    public Map<String, Object> complaintTrend(@RequestParam(defaultValue = "7") int days) {
        return statsService.complaintTrend(days);
    }

    @Operation(summary = "辖区垃圾房实时状态总览")
    @GetMapping("/rooms")
    public List<?> rooms() {
        return roomRepo.findAll().stream().map(viewMapper::room).toList();
    }

    @Operation(summary = "全部联动留痕（按垃圾房倒序）")
    @GetMapping("/linkage-logs")
    public List<?> logs(@RequestParam(required = false) Long roomId) {
        if (roomId != null) {
            return linkageLogRepo.findTop100ByBinRoomIdOrderByCreatedAtDesc(roomId).stream()
                    .map(viewMapper::linkageLog).toList();
        }
        return linkageLogRepo.findAll().stream()
                .sorted(java.util.Comparator.comparing(
                        com.city.binroom.domain.entity.LinkageLog::getCreatedAt).reversed())
                .limit(200)
                .map(viewMapper::linkageLog).toList();
    }
}
