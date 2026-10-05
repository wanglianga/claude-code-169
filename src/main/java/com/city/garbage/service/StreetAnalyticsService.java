package com.city.garbage.service;

import com.city.garbage.domain.*;
import com.city.garbage.repo.*;
import com.city.garbage.support.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * 街道端：按小区查看分类质量（投放量/正确率/错投）与投诉趋势，
 * 数据全部来自真实投放、清运、投诉与联动闭环记录。
 */
@Service
@RequiredArgsConstructor
public class StreetAnalyticsService {

    private final CommunityRepository communityRepository;
    private final DisposalRepository disposalRepository;
    private final ComplaintRepository complaintRepository;
    private final GarbageRoomRepository roomRepository;
    private final GovernanceCaseRepository caseRepository;
    private final TimeService timeService;

    public Map<String, Object> communityQuality(String communityCode, int days) {
        Community community = communityRepository.findByCode(communityCode)
                .orElseThrow(() -> new IllegalArgumentException("小区不存在: " + communityCode));
        OffsetDateTime now = timeService.now();
        OffsetDateTime from = now.minusDays(days).toLocalDate().atStartOfDay(timeService.zone()).toOffsetDateTime();

        List<DisposalRecord> records =
                disposalRepository.findByCommunityIdAndCreatedAtBetween(community.getId(), from, now);
        List<Complaint> complaints = complaintRepository
                .findByCommunityIdAndCreatedAtBetween(community.getId(), from, now);

        long total = records.size();
        long wrong = records.stream().filter(DisposalRecord::isWrongSort).count();
        double correctRate = total == 0 ? 0 : Math.round((total - wrong) * 10000.0 / total) / 100.0;
        double totalWeight = records.stream().filter(r -> r.getWeightKg() != null)
                .mapToDouble(DisposalRecord::getWeightKg).sum();

        List<GarbageRoom> rooms = roomRepository.findByCommunityId(community.getId());
        long activeCases = rooms.stream().filter(r -> r.getActiveCaseNo() != null).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("communityCode", community.getCode());
        result.put("communityName", community.getName());
        result.put("streetName", community.getStreetName());
        result.put("windowDays", days);
        result.put("roomCount", rooms.size());
        result.put("totalDisposals", total);
        result.put("totalWeightKg", Math.round(totalWeight * 100) / 100.0);
        result.put("wrongSortCount", wrong);
        result.put("correctRatePct", correctRate);
        result.put("complaintCount", complaints.size());
        result.put("complaintOpenCount", complaints.stream().filter(c -> c.getStatus() != ComplaintStatus.RESOLVED).count());
        result.put("activeGovernanceCases", activeCases);
        result.put("dailyTrend", dailyTrend(records, complaints, from.toLocalDate(), now.toLocalDate()));
        return result;
    }

    public List<Map<String, Object>> overview(int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Community c : communityRepository.findAll()) {
            list.add(communityQuality(c.getCode(), days));
        }
        list.sort(Comparator.comparingInt(m -> -((Number) m.get("wrongSortCount")).intValue()));
        return list;
    }

    private List<Map<String, Object>> dailyTrend(List<DisposalRecord> records, List<Complaint> complaints,
                                                 LocalDate from, LocalDate to) {
        Map<LocalDate, long[]> buckets = new TreeMap<>(); // [total, wrong]
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            buckets.put(d, new long[2]);
        }
        for (DisposalRecord r : records) {
            if (r.getCreatedAt() == null) continue;
            LocalDate d = r.getCreatedAt().toLocalDate();
            long[] v = buckets.get(d);
            if (v != null) {
                v[0]++;
                if (r.isWrongSort()) v[1]++;
            }
        }
        Map<LocalDate, Long> complaintPerDay = new TreeMap<>();
        for (Complaint c : complaints) {
            if (c.getCreatedAt() == null) continue;
            complaintPerDay.merge(c.getCreatedAt().toLocalDate(), 1L, Long::sum);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> e : buckets.entrySet()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", e.getKey().toString());
            day.put("disposals", e.getValue()[0]);
            day.put("wrongSort", e.getValue()[1]);
            day.put("correctRatePct", e.getValue()[0] == 0 ? 100.0
                    : Math.round((e.getValue()[0] - e.getValue()[1]) * 10000.0 / e.getValue()[0]) / 100.0);
            day.put("complaints", complaintPerDay.getOrDefault(e.getKey(), 0L));
            trend.add(day);
        }
        return trend;
    }
}
