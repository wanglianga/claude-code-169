package com.city.binroom.web.dto;

import com.city.binroom.domain.entity.*;
import com.city.binroom.service.RoomStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** 实体 -> 前端视图对象，集中处理关联信息与垃圾房综合状态 */
@Component
@RequiredArgsConstructor
public class ViewMapper {

    private final RoomStateService stateService;

    public Map<String, Object> community(Community c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("street", c.getStreet());
        m.put("propertyCompany", c.getPropertyCompany());
        return m;
    }

    public Map<String, Object> room(BinRoom r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("address", r.getAddress());
        m.put("communityId", r.getCommunity().getId());
        m.put("communityName", r.getCommunity().getName());
        m.put("street", r.getCommunity().getStreet());
        m.put("kitchenFillPct", r.getKitchenFillPct());
        m.put("otherFillPct", r.getOtherFillPct());
        m.put("odorLevel", r.getOdorLevel());
        m.put("online", r.getOnline());
        m.put("lastHeartbeatAt", r.getLastHeartbeatAt());
        m.put("kitchenFullSince", r.getKitchenFullSince());
        m.put("truckOnSite", r.getTruckOnSite());
        m.put("lastPickupAt", r.getLastPickupAt());
        m.put("lastCleanedAt", r.getLastCleanedAt());
        m.put("nightPeak", r.getNightPeak());
        m.put("state", stateService.stateOf(r).name());
        m.put("stateLabel", stateService.stateOf(r).getLabel());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    public Map<String, Object> resident(Resident r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("cardNo", r.getCardNo());
        m.put("name", r.getName());
        m.put("phone", r.getPhone());
        m.put("communityName", r.getBuilding().getCommunity().getName());
        m.put("buildingName", r.getBuilding().getName());
        m.put("pointsBalance", r.getPointsBalance());
        return m;
    }

    public Map<String, Object> disposal(DisposalRecord d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("bizNo", d.getBizNo());
        m.put("residentName", d.getResident().getName());
        m.put("cardNo", d.getResident().getCardNo());
        m.put("communityName", d.getResident().getBuilding().getCommunity().getName());
        m.put("buildingName", d.getResident().getBuilding().getName());
        m.put("roomCode", d.getBinRoom().getCode());
        m.put("roomName", d.getBinRoom().getName());
        m.put("wasteType", d.getWasteType().name());
        m.put("wasteTypeLabel", d.getWasteType().getLabel());
        m.put("accessMethod", d.getAccessMethod().name());
        m.put("accessMethodLabel", d.getAccessMethod().getLabel());
        m.put("weightKg", d.getWeightKg());
        m.put("aiVerdict", d.getAiVerdict().name());
        m.put("aiVerdictLabel", d.getAiVerdict().getLabel());
        m.put("violationTip", d.getViolationTip());
        m.put("basePoints", d.getBasePoints());
        m.put("penaltyPoints", d.getPenaltyPoints());
        m.put("pointsCredited", d.getPointsCredited());
        m.put("pointsWithheld", d.getPointsWithheld());
        m.put("pointsSettled", d.getPointsSettled());
        m.put("creditDelayed", d.getCreditDelayed());
        m.put("createdAt", d.getCreatedAt());
        return m;
    }

    public Map<String, Object> ledger(PointsLedger l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("direction", l.getDirection().name());
        m.put("directionLabel", l.getDirection().getLabel());
        m.put("points", l.getPoints());
        m.put("disposalBizNo", l.getDisposalBizNo());
        m.put("reason", l.getReason());
        m.put("createdAt", l.getCreatedAt());
        return m;
    }

    public Map<String, Object> cleaning(CleaningTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("taskNo", t.getTaskNo());
        m.put("roomId", t.getBinRoom().getId());
        m.put("roomCode", t.getBinRoom().getCode());
        m.put("roomName", t.getBinRoom().getName());
        m.put("communityName", t.getBinRoom().getCommunity().getName());
        m.put("taskKind", t.getTaskKind());
        m.put("reason", t.getReason());
        m.put("linkageType", t.getLinkageType());
        m.put("status", t.getStatus().name());
        m.put("statusLabel", t.getStatus().getLabel());
        m.put("assignee", t.getAssignee());
        m.put("expectedBefore", t.getExpectedBefore());
        m.put("assignedAt", t.getAssignedAt());
        m.put("completedAt", t.getCompletedAt());
        m.put("resultNote", t.getResultNote());
        m.put("escalated", t.getEscalated());
        m.put("sourceTaskNo", t.getSourceTaskNo());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    public Map<String, Object> pickup(PickupTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("taskNo", t.getTaskNo());
        m.put("roomId", t.getBinRoom().getId());
        m.put("roomCode", t.getBinRoom().getCode());
        m.put("roomName", t.getBinRoom().getName());
        m.put("communityName", t.getBinRoom().getCommunity().getName());
        m.put("wasteType", t.getWasteType().name());
        m.put("wasteTypeLabel", t.getWasteType().getLabel());
        m.put("priorityScore", t.getPriorityScore());
        m.put("reason", t.getReason());
        m.put("linkageType", t.getLinkageType());
        m.put("status", t.getStatus().name());
        m.put("statusLabel", t.getStatus().getLabel());
        m.put("assignee", t.getAssignee());
        m.put("routeCode", t.getRouteCode());
        m.put("expectedBefore", t.getExpectedBefore());
        m.put("arrivedAt", t.getArrivedAt());
        m.put("completedAt", t.getCompletedAt());
        m.put("pickedWeightKg", t.getPickedWeightKg());
        m.put("cleanedAfterPickup", t.getCleanedAfterPickup());
        m.put("escalated", t.getEscalated());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    public Map<String, Object> complaint(Complaint c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("complaintNo", c.getComplaintNo());
        m.put("residentName", c.getResident() == null ? "匿名" : c.getResident().getName());
        m.put("roomCode", c.getBinRoom().getCode());
        m.put("roomName", c.getBinRoom().getName());
        m.put("communityName", c.getBinRoom().getCommunity().getName());
        m.put("type", c.getType().name());
        m.put("typeLabel", c.getType().getLabel());
        m.put("content", c.getContent());
        m.put("status", c.getStatus().name());
        m.put("statusLabel", c.getStatus().getLabel());
        m.put("cleaningTaskNo", c.getCleaningTask() == null ? null : c.getCleaningTask().getTaskNo());
        m.put("handleNote", c.getHandleNote());
        m.put("handledAt", c.getHandledAt());
        m.put("createdAt", c.getCreatedAt());
        return m;
    }

    public Map<String, Object> rectification(Rectification r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("rectifyNo", r.getRectifyNo());
        m.put("disposalBizNo", r.getDisposal().getBizNo());
        m.put("roomCode", r.getBinRoom().getCode());
        m.put("roomName", r.getBinRoom().getName());
        m.put("communityName", r.getBinRoom().getCommunity().getName());
        m.put("residentName", r.getDisposal().getResident().getName());
        m.put("kind", r.getKind());
        m.put("wasteTypeLabel", r.getDisposal().getWasteType().getLabel());
        m.put("weightKg", r.getDisposal().getWeightKg());
        m.put("aiVerdictLabel", r.getDisposal().getAiVerdict().getLabel());
        m.put("appealReason", r.getAppealReason());
        m.put("result", r.getResult().name());
        m.put("resultLabel", r.getResult().getLabel());
        m.put("reviewNote", r.getReviewNote());
        m.put("reviewer", r.getReviewer());
        m.put("reviewedAt", r.getReviewedAt());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    public Map<String, Object> linkageLog(LinkageLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("roomCode", l.getBinRoom().getCode());
        m.put("roomName", l.getBinRoom().getName());
        m.put("linkageType", l.getLinkageType());
        m.put("action", l.getAction());
        m.put("detail", l.getDetail());
        m.put("refBizNo", l.getRefBizNo());
        m.put("createdAt", l.getCreatedAt());
        return m;
    }
}
