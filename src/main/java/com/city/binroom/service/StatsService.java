package com.city.binroom.service;

import com.city.binroom.domain.entity.Complaint;
import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.enums.ComplaintType;
import com.city.binroom.domain.enums.TaskStatus;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.ComplaintRepository;
import com.city.binroom.domain.repo.DisposalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 街道端统计：按小区分类质量、投诉趋势 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final DisposalRecordRepository disposalRepo;
    private final ComplaintRepository complaintRepo;

    /** 近 days 天按小区的分类质量 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> classificationQuality(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<DisposalRecord> records = disposalRepo.findByCreatedAtBetween(from, LocalDateTime.now());

        Map<String, QualityAgg> byCommunity = new TreeMap<>();
        for (DisposalRecord d : records) {
            String communityName = d.getResident().getBuilding().getCommunity().getName();
            QualityAgg agg = byCommunity.computeIfAbsent(communityName, QualityAgg::new);
            agg.add(d);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (QualityAgg agg : byCommunity.values()) {
            result.add(agg.toView());
        }
        return result;
    }

    /** 近 days 天投诉趋势（按日期 × 类型） */
    @Transactional(readOnly = true)
    public Map<String, Object> complaintTrend(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Complaint> complaints = complaintRepo.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(from))
                .toList();

        List<String> dates = new ArrayList<>();
        Map<LocalDate, Integer> byDate = new TreeMap<>();
        Map<String, Map<LocalDate, Integer>> byType = new TreeMap<>();
        Map<String, Long> totalsByType = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).toString());
        }
        for (Complaint c : complaints) {
            LocalDate day = c.getCreatedAt().toLocalDate();
            byDate.merge(day, 1, Integer::sum);
            byType.computeIfAbsent(c.getType().name(), k -> new TreeMap<>())
                    .merge(day, 1, Integer::sum);
            totalsByType.merge(c.getType().name(), 1L, Long::sum);
        }
        List<Integer> totals = dates.stream()
                .map(d -> byDate.getOrDefault(LocalDate.parse(d), 0)).toList();

        List<Map<String, Object>> series = new ArrayList<>();
        for (var e : byType.entrySet()) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type", e.getKey());
            s.put("typeLabel", ComplaintType.valueOf(e.getKey()).getLabel());
            s.put("counts", dates.stream()
                    .map(d -> e.getValue().getOrDefault(LocalDate.parse(d), 0)).toList());
            series.add(s);
        }
        long pending = complaints.stream().filter(c -> c.getStatus() != TaskStatus.DONE
                && c.getStatus() != TaskStatus.CANCELLED).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("totals", totals);
        result.put("series", series);
        result.put("totalsByType", totalsByType);
        result.put("totalComplaints", complaints.size());
        result.put("pendingComplaints", pending);
        return result;
    }

    private static class QualityAgg {
        String communityName;
        int total;
        int correct;
        int misSort;
        int suspected;
        int unknown;
        double kitchenWeight;
        double otherWeight;
        int pointsCredited;
        int penaltyPoints;

        QualityAgg(String communityName) {
            this.communityName = communityName;
        }

        void add(DisposalRecord d) {
            total++;
            switch (d.getAiVerdict()) {
                case CORRECT -> correct++;
                case MIS_SORT -> misSort++;
                case SUSPECTED -> suspected++;
                default -> unknown++;
            }
            if (d.getWasteType() == WasteType.KITCHEN) {
                kitchenWeight += d.getWeightKg();
            } else if (d.getWasteType() == WasteType.OTHER) {
                otherWeight += d.getWeightKg();
            }
            if (Boolean.TRUE.equals(d.getPointsCredited())) {
                pointsCredited += d.getBasePoints();
            }
            if (Boolean.TRUE.equals(d.getPenaltyApplied())) {
                penaltyPoints += d.getPenaltyPoints();
            }
        }

        Map<String, Object> toView() {
            int judged = correct + misSort + suspected;
            double rate = judged == 0 ? 100.0 : Math.round(correct * 10000.0 / judged) / 100.0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("communityName", communityName);
            m.put("total", total);
            m.put("correct", correct);
            m.put("misSort", misSort);
            m.put("suspected", suspected);
            m.put("unknown", unknown);
            m.put("kitchenWeightKg", Math.round(kitchenWeight * 100) / 100.0);
            m.put("otherWeightKg", Math.round(otherWeight * 100) / 100.0);
            m.put("pointsCredited", pointsCredited);
            m.put("penaltyPoints", penaltyPoints);
            m.put("correctRatePct", rate);
            return m;
        }
    }
}
