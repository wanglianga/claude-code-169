package com.city.garbage.api;

import com.city.garbage.domain.Alert;
import com.city.garbage.domain.GarbageRoom;
import com.city.garbage.domain.WorkTask;
import com.city.garbage.repo.AlertRepository;
import com.city.garbage.repo.GarbageRoomRepository;
import com.city.garbage.service.WorkTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 垃圾房统一状态视图：物业、环卫、街道、居民四端围绕同一份状态更新。
 */
@Tag(name = "垃圾房状态", description = "四端共享的垃圾房实时状态、活跃告警与任务")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final GarbageRoomRepository roomRepository;
    private final AlertRepository alertRepository;
    private final WorkTaskService taskService;

    @Operation(summary = "全部垃圾房实时状态")
    @GetMapping
    public List<GarbageRoom> all() {
        return roomRepository.findAll();
    }

    @Operation(summary = "垃圾房统一联动视图：实时状态 + 活跃告警 + 治理任务 + 联动单号")
    @GetMapping("/{roomCode}")
    public Map<String, Object> detail(@PathVariable String roomCode) {
        GarbageRoom room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new com.city.garbage.support.BusinessException("垃圾房不存在: " + roomCode));
        List<Alert> alerts = alertRepository.findByRoomCodeAndStatusIn(roomCode,
                List.of(com.city.garbage.domain.AlertStatus.OPEN, com.city.garbage.domain.AlertStatus.LINKED));
        List<WorkTask> tasks = alerts.stream().findFirst()
                .map(a -> taskService.byCase(alerts.get(0).getCaseNo()))
                .orElseGet(() -> room.getActiveCaseNo() != null ? taskService.byCase(room.getActiveCaseNo()) : List.of());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("room", room);
        view.put("activeAlerts", alerts);
        view.put("tasks", tasks);
        return view;
    }
}
