package com.city.binroom.web;

import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.LinkageLogRepository;
import com.city.binroom.domain.repo.PickupTaskRepository;
import com.city.binroom.service.GovernanceTaskService;
import com.city.binroom.service.PickupLifecycleService;
import com.city.binroom.web.dto.ViewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Tag(name = "环卫端")
@RestController
@RequestMapping("/api/sanitation")
@RequiredArgsConstructor
public class SanitationController {

    private static final List<TaskStatus> OPEN = List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED,
            TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE, TaskStatus.ESCALATED);

    private final PickupTaskRepository pickupRepo;
    private final BinRoomRepository roomRepo;
    private final LinkageLogRepository linkageLogRepo;
    private final PickupLifecycleService pickupService;
    private final GovernanceTaskService taskService;
    private final ViewMapper viewMapper;

    @Operation(summary = "清运优先级队列：按优先级评分降序，环卫调度依据")
    @GetMapping("/pickup-tasks")
    public List<?> pickupQueue() {
        taskService.recalcPriorities();
        return pickupRepo.findByStatusIn(OPEN).stream()
                .map(viewMapper::pickup)
                .sorted(Comparator.comparingInt((java.util.Map<String, Object> t) ->
                        (Integer) t.get("priorityScore")).reversed())
                .toList();
    }

    @Operation(summary = "派车并指定路线")
    @PostMapping("/pickup-tasks/{id}/assign")
    public Object assign(@PathVariable Long id, @RequestBody AssignRequest body) {
        return viewMapper.pickup(
                pickupService.assign(id, body.getAssignee(), body.getRouteCode()));
    }

    @Operation(summary = "清运车到场（满溢等待解除）")
    @PostMapping("/pickup-tasks/{id}/arrive")
    public Object arrive(@PathVariable Long id) {
        return viewMapper.pickup(pickupService.arrive(id));
    }

    @Operation(summary = "清运完成（容量清零并生成清运后清洁任务）")
    @PostMapping("/pickup-tasks/{id}/complete")
    public Object complete(@PathVariable Long id, @RequestBody CompleteRequest body) {
        return viewMapper.pickup(
                pickupService.complete(id, body.getPickedWeightKg(), body.getNote()));
    }

    @Operation(summary = "全部垃圾房容量与状态")
    @GetMapping("/rooms")
    public List<?> rooms() {
        return roomRepo.findAll().stream().map(viewMapper::room).toList();
    }

    @Operation(summary = "垃圾房联动留痕")
    @GetMapping("/rooms/{roomId}/linkage-logs")
    public List<?> logs(@PathVariable Long roomId) {
        return linkageLogRepo.findTop100ByBinRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(viewMapper::linkageLog).toList();
    }

    @Data
    public static class AssignRequest {
        @NotBlank
        private String assignee;
        private String routeCode;
    }

    @Data
    public static class CompleteRequest {
        @NotNull
        private Double pickedWeightKg;
        @NotBlank
        private String note;
    }
}
