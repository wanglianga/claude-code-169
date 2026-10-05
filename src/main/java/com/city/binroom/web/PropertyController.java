package com.city.binroom.web;

import com.city.binroom.domain.entity.SysUser;
import com.city.binroom.domain.enums.RectifyResult;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.repo.*;
import com.city.binroom.service.CleaningLifecycleService;
import com.city.binroom.service.RectificationService;
import com.city.binroom.web.auth.CurrentUser;
import com.city.binroom.web.dto.ViewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "物业端")
@RestController
@RequestMapping("/api/property")
@RequiredArgsConstructor
public class PropertyController {

    private static final List<TaskStatus> OPEN = List.of(TaskStatus.PENDING, TaskStatus.DISPATCHED,
            TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE, TaskStatus.ESCALATED);

    private final CleaningTaskRepository cleaningRepo;
    private final ComplaintRepository complaintRepo;
    private final RectificationRepository rectificationRepo;
    private final LinkageLogRepository linkageLogRepo;
    private final BinRoomRepository roomRepo;
    private final SysUserRepository sysUserRepo;
    private final CleaningLifecycleService cleaningService;
    private final RectificationService rectificationService;
    private final ViewMapper viewMapper;

    @Operation(summary = "待处理任务（保洁/消杀/维修/巡查/清运后清洁），物业端工作台")
    @GetMapping("/tasks")
    public List<?> tasks(HttpServletRequest request, @RequestParam(required = false) String status) {
        Long scopeCommunityId = scopeCommunityId(request);
        List<TaskStatus> statuses = status == null ? OPEN : List.of(TaskStatus.valueOf(status));
        return cleaningRepo.findByStatusInOrderByCreatedAtAsc(statuses).stream()
                .filter(t -> inScope(scopeCommunityId, t.getBinRoom().getCommunity().getId()))
                .map(viewMapper::cleaning).toList();
    }

    @Operation(summary = "派单给保洁人员")
    @PostMapping("/tasks/{id}/assign")
    public Object assign(@PathVariable Long id, @RequestBody AssignRequest body) {
        return viewMapper.cleaning(cleaningService.assign(id, body.getAssignee()));
    }

    @Operation(summary = "保洁/消杀完成，联动闭环同来源投诉")
    @PostMapping("/tasks/{id}/complete")
    public Object complete(@PathVariable Long id, @RequestBody CompleteRequest body) {
        return viewMapper.cleaning(cleaningService.complete(id, body.getNote()));
    }

    @Operation(summary = "投诉工单列表")
    @GetMapping("/complaints")
    public List<?> complaints(HttpServletRequest request) {
        Long scope = scopeCommunityId(request);
        return complaintRepo.findAll().stream()
                .filter(c -> inScope(scope, c.getBinRoom().getCommunity().getId()))
                .map(viewMapper::complaint).toList();
    }

    @Operation(summary = "待核查整改/申诉（错投整改与摄像头误判申诉）")
    @GetMapping("/rectifications")
    public List<?> rectifications(HttpServletRequest request) {
        Long scope = scopeCommunityId(request);
        return rectificationRepo.findAll().stream()
                .filter(r -> inScope(scope, r.getBinRoom().getCommunity().getId()))
                .map(viewMapper::rectification).toList();
    }

    @Operation(summary = "核查整改/申诉：PASS 通过(撤销违规返还积分) / FAIL 不合格 / WAIVED 误判撤销")
    @PostMapping("/rectifications/{rectifyNo}/review")
    public Object review(@PathVariable String rectifyNo, @RequestBody ReviewRequest body,
                         HttpServletRequest request) {
        CurrentUser cu = (CurrentUser) request.getAttribute(CurrentUser.ATTR_USER);
        String reviewer = sysUserRepo.findById(cu.getUserId()).map(SysUser::getDisplayName).orElse("物业");
        return viewMapper.rectification(rectificationService.review(
                rectifyNo, RectifyResult.valueOf(body.getResult()), body.getNote(), reviewer));
    }

    @Operation(summary = "本小区垃圾房实时状态")
    @GetMapping("/rooms")
    public List<?> rooms(HttpServletRequest request) {
        Long scope = scopeCommunityId(request);
        return roomRepo.findAll().stream()
                .filter(r -> inScope(scope, r.getCommunity().getId()))
                .map(viewMapper::room).toList();
    }

    @Operation(summary = "垃圾房联动留痕")
    @GetMapping("/rooms/{roomId}/linkage-logs")
    public List<?> logs(@PathVariable Long roomId) {
        return linkageLogRepo.findTop100ByBinRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(viewMapper::linkageLog).toList();
    }

    /** ADMIN 可看全部；物业账号仅本小区 */
    private Long scopeCommunityId(HttpServletRequest request) {
        CurrentUser cu = (CurrentUser) request.getAttribute(CurrentUser.ATTR_USER);
        if ("ADMIN".equals(cu.getRole())) {
            return null;
        }
        return sysUserRepo.findById(cu.getUserId()).map(SysUser::getCommunityId).orElse(-1L);
    }

    private boolean inScope(Long scopeCommunityId, Long communityId) {
        return scopeCommunityId == null || scopeCommunityId.equals(communityId);
    }

    @Data
    public static class AssignRequest {
        @NotBlank
        private String assignee;
    }

    @Data
    public static class CompleteRequest {
        @NotBlank
        private String note;
    }

    @Data
    public static class ReviewRequest {
        @NotBlank
        private String result;
        @NotBlank
        private String note;
    }
}
