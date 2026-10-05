package com.city.garbage.api;

import com.city.garbage.domain.TruckVisit;
import com.city.garbage.service.SanitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 环卫端：清运车到场/离场由设备 TRUCK_ARRIVAL 事件上报，
 * 这里看清运优先级、做清运后清洁确认。
 */
@Tag(name = "环卫端", description = "清运优先级、清运后清洁确认")
@RestController
@RequestMapping("/api/sanitation")
@RequiredArgsConstructor
public class SanitationController {

    private final SanitationService sanitationService;

    @Operation(summary = "清运优先级列表（厨余满溢未到场 > 其他垃圾满溢 > 容量/满溢时长）")
    @GetMapping("/truck-priorities")
    public List<Map<String, Object>> priorities() {
        return sanitationService.priorityList();
    }

    @Operation(summary = "清运完成后清洁确认（不确认超过阈值将产生清运后未清洁告警）")
    @PostMapping("/rooms/{roomCode}/clean-confirm")
    public TruckVisit confirmCleaned(@PathVariable String roomCode,
                                     @RequestParam(required = false) String note) {
        return sanitationService.confirmCleaned(roomCode, note);
    }
}
