package com.city.binroom;

import com.city.binroom.bootstrap.SeedDataRunner;
import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.Complaint;
import com.city.binroom.domain.entity.DisposalRecord;
import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.entity.PointsLedger;
import com.city.binroom.domain.entity.Rectification;
import com.city.binroom.domain.enums.*;
import com.city.binroom.domain.repo.*;
import com.city.binroom.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 九类联动场景集成测试（local 事件模式，H2 数据库）：
 * 设备告警经事件总线驱动保洁/清运/投诉/整改/积分闭环。
 */
@ActiveProfiles("test")
@SpringBootTest
class LinkageScenariosTest {

    @Autowired private DeviceIngestService deviceIngest;
    @Autowired private DisposalService disposalService;
    @Autowired private ComplaintService complaintService;
    @Autowired private RectificationService rectificationService;
    @Autowired private CleaningLifecycleService cleaningLifecycle;
    @Autowired private PickupLifecycleService pickupLifecycle;
    @Autowired private GovernanceTaskService taskService;
    @Autowired private LinkageSweepService sweepService;
    @Autowired private RoomStateService roomStateService;
    @Autowired private StatsService statsService;

    @Autowired private BinRoomRepository roomRepo;
    @Autowired private CleaningTaskRepository cleaningRepo;
    @Autowired private PickupTaskRepository pickupRepo;
    @Autowired private ComplaintRepository complaintRepo;
    @Autowired private DisposalRecordRepository disposalRepo;
    @Autowired private RectificationRepository rectificationRepo;
    @Autowired private ResidentRepository residentRepo;
    @Autowired private PointsLedgerRepository ledgerRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SeedDataRunner seedDataRunner;

    private BinRoom br001;
    private BinRoom br002;
    private BinRoom br003;

    @BeforeEach
    void setUp() {
        // 每个测试用例重置为干净的种子数据（事件处理器需在真实事务提交后运行，不能用测试事务回滚）
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of("linkage_log", "rectification", "complaint", "points_ledger",
                "disposal_record", "pickup_task", "cleaning_task", "device_event", "auth_token",
                "resident", "bin_room", "building", "community", "sys_user")) {
            jdbc.execute("DELETE FROM " + table);
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        seedDataRunner.run(new org.springframework.boot.DefaultApplicationArguments());

        br001 = roomRepo.findByCode("BR-001").orElseThrow();
        br002 = roomRepo.findByCode("BR-002").orElseThrow();
        br003 = roomRepo.findByCode("BR-003").orElseThrow();
    }

    @Test
    @DisplayName("场景1：厨余满但清运车未到 → 清运派单 → 超时升级 → 到场清运 → 清运后清洁超时升级")
    void fullNoTruckEscalatePickupThenPostClean() {
        // 厨余桶上报 95%：自动生成清运任务
        deviceIngest.report("BR-001", DeviceEventType.CAPACITY, 95.0, "KITCHEN", null);
        PickupTask pickup = taskService.findOpenPickup(br001.getId(), WasteType.KITCHEN).orElseThrow();
        assertEquals(TaskStatus.PENDING, pickup.getStatus());

        // 巡检：满溢后清运车超时未到，升级加急
        sweepService.runSweep();
        pickup = pickupRepo.findById(pickup.getId()).orElseThrow();
        assertEquals(TaskStatus.ESCALATED, pickup.getStatus());
        assertTrue(pickup.getEscalated());

        // 环卫派车、到场、清运
        pickupLifecycle.assign(pickup.getId(), "司机-老刘", "滨河-3号线");
        pickupLifecycle.arrive(pickup.getId());
        BinRoom room = roomRepo.findById(br001.getId()).orElseThrow();
        assertTrue(room.getTruckOnSite());
        pickupLifecycle.complete(pickup.getId(), 160.0, "厨余清运完成");

        room = roomRepo.findById(br001.getId()).orElseThrow();
        assertEquals(10, room.getKitchenFillPct());
        assertNull(room.getKitchenFullSince());
        assertNotNull(room.getLastPickupAt());

        // 自动生成清运后清洁任务，超时未清洁 → 升级督办
        CleaningTask postClean = taskService.findOpenCleaning(br001.getId(), "POST_PICKUP_CLEAN").orElseThrow();
        assertEquals(LinkageType.NOT_CLEANED_AFTER_PICKUP.name(), postClean.getLinkageType());
        sleep(10);
        sweepService.runSweep();
        postClean = cleaningRepo.findById(postClean.getId()).orElseThrow();
        assertEquals(TaskStatus.ESCALATED, postClean.getStatus());

        // 物业完成清洁
        cleaningLifecycle.assign(postClean.getId(), "保洁-赵姐");
        cleaningLifecycle.complete(postClean.getId(), "清运后冲洗消杀完成");
        assertEquals(TaskStatus.DONE, cleaningRepo.findById(postClean.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("场景2：异味告警 → 消杀派单；异味投诉 → 同一消杀任务，完成后投诉闭环；满溢+异味同批治理")
    void odorAlertAndComplaintCloseTogether() {
        deviceIngest.report("BR-001", DeviceEventType.ODOR, 78.0, null, null);
        CleaningTask disinfect = taskService.findOpenCleaning(br001.getId(), "DISINFECT").orElseThrow();

        // 满溢与异味同时存在：清运任务也应被补齐
        deviceIngest.report("BR-001", DeviceEventType.CAPACITY, 94.0, "KITCHEN", null);
        assertNotNull(taskService.findOpenPickup(br001.getId(), WasteType.KITCHEN).orElse(null));

        Complaint complaint = complaintService.create(
                residentRepo.findByCardNo("R1002").orElseThrow().getId(),
                "BR-001", ComplaintType.ODOR, "厨余桶异味刺鼻，影响通行");
        complaint = complaintRepo.findById(complaint.getId()).orElseThrow();
        assertEquals(disinfect.getId(), complaint.getCleaningTask().getId());

        cleaningLifecycle.complete(disinfect.getId(), "消杀完成异味消除");
        complaint = complaintRepo.findById(complaint.getId()).orElseThrow();
        assertEquals(TaskStatus.DONE, complaint.getStatus());
        assertEquals(10, roomRepo.findById(br001.getId()).orElseThrow().getOdorLevel());
    }

    @Test
    @DisplayName("场景3：居民错投被识别 → 暂扣积分+违规扣分+整改单；申诉误判成立 → 返还扣分并补发积分")
    void misSortThenAppealWaivedRestoresPoints() {
        var before = residentRepo.findByCardNo("R1001").orElseThrow().getPointsBalance();

        DisposalRecord d = disposalService.create("R1001", "BR-001", WasteType.KITCHEN,
                AccessMethod.CARD, 1.0, AiVerdict.MIS_SORT, false);
        assertTrue(d.getPointsWithheld());
        assertEquals(20, d.getBasePoints());
        assertEquals(20, d.getPenaltyPoints());
        assertEquals(before - 20, residentRepo.findByCardNo("R1001").orElseThrow().getPointsBalance());
        Rectification violation = rectificationRepo
                .findByDisposalIdAndKind(d.getId(), "VIOLATION").orElseThrow();
        assertEquals(RectifyResult.PENDING, violation.getResult());

        // 居民申诉摄像头误判，物业核查 WAIVED
        rectificationService.appeal(d.getBizNo(), "投放的是剩菜剩饭，摄像头角度遮挡误判");
        Rectification appeal = rectificationRepo
                .findByDisposalIdAndKind(d.getId(), "APPEAL").orElseThrow();
        rectificationService.review(appeal.getRectifyNo(), RectifyResult.WAIVED,
                "现场录像复核确认为厨余，识别误判", "物业管家-小陈");

        Integer after = residentRepo.findByCardNo("R1001").orElseThrow().getPointsBalance();
        assertEquals(before + 20, after, "违规扣分返还且暂扣的 20 分补发");
        DisposalRecord dAfter = disposalRepo.findById(d.getId()).orElseThrow();
        assertTrue(dAfter.getPointsCredited());
        assertFalse(dAfter.getPointsWithheld());
        // 积分流水含扣分与补发两类，且都关联同一投放单号
        String bizNo = d.getBizNo();
        List<PointsLedger> ledger = ledgerRepo.findAll().stream()
                .filter(l -> bizNo.equals(l.getDisposalBizNo())).toList();
        assertTrue(ledger.stream().anyMatch(l -> l.getDirection() == PointsDirection.PENALTY));
        assertTrue(ledger.stream().anyMatch(l -> l.getDirection() == PointsDirection.MAKEUP));
    }

    @Test
    @DisplayName("场景4：夜间投放高峰（30分钟3次）→ 物业巡查 + 环卫清运待命")
    void nightPeakDispatchesPatrolAndStandby() {
        for (int i = 0; i < 3; i++) {
            disposalService.create("R1003", "BR-002", WasteType.KITCHEN,
                    AccessMethod.QRCODE, 0.6, AiVerdict.CORRECT, false);
        }
        assertTrue(roomRepo.findById(br002.getId()).orElseThrow().getNightPeak());
        CleaningTask patrol = taskService.findOpenCleaning(br002.getId(), "NIGHT_PATROL").orElseThrow();
        assertEquals(LinkageType.NIGHT_PEAK.name(), patrol.getLinkageType());
        PickupTask standby = taskService.findOpenPickup(br002.getId(), WasteType.KITCHEN).orElseThrow();
        assertEquals(LinkageType.NIGHT_PEAK.name(), standby.getLinkageType());
    }

    @Test
    @DisplayName("场景5：设备断网 → 派维修任务；心跳恢复后不重复派单")
    void offlineDispatchRepairAndHeartbeatRecovers() {
        BinRoom room = roomRepo.findById(br003.getId()).orElseThrow();
        room.setLastHeartbeatAt(LocalDateTime.now().minusHours(2));
        roomRepo.save(room);

        sweepService.runSweep();
        assertFalse(roomRepo.findById(br003.getId()).orElseThrow().getOnline());
        CleaningTask repair = taskService.findOpenCleaning(br003.getId(), "DEVICE_REPAIR").orElseThrow();
        assertEquals(LinkageType.DEVICE_OFFLINE.name(), repair.getLinkageType());

        // 设备恢复上报心跳
        deviceIngest.report("BR-003", DeviceEventType.HEARTBEAT, 1.0, null, null);
        assertTrue(roomRepo.findById(br003.getId()).orElseThrow().getOnline());
        long repairCount = cleaningRepo.findAll().stream()
                .filter(t -> t.getBinRoom().getId().equals(br003.getId())
                        && "DEVICE_REPAIR".equals(t.getTaskKind())).count();
        assertEquals(1, repairCount, "同一断网周期不得重复派维修单");
    }

    @Test
    @DisplayName("场景6：积分未到账（设备断网/链路延迟）→ 超时巡检自动补发，流水可追溯")
    void missingPointsAutoMakeup() {
        int before = residentRepo.findByCardNo("R2001").orElseThrow().getPointsBalance();
        DisposalRecord d = disposalService.create("R2001", "BR-003", WasteType.KITCHEN,
                AccessMethod.CARD, 1.5, AiVerdict.CORRECT, true);
        assertFalse(disposalRepo.findById(d.getId()).orElseThrow().getPointsCredited());
        assertEquals(before, residentRepo.findByCardNo("R2001").orElseThrow().getPointsBalance());

        sleep(10);
        sweepService.runSweep();
        DisposalRecord dAfter = disposalRepo.findById(d.getId()).orElseThrow();
        assertTrue(dAfter.getPointsCredited());
        assertEquals(before + 30, residentRepo.findByCardNo("R2001").orElseThrow().getPointsBalance());
        assertTrue(ledgerRepo.findAll().stream()
                .anyMatch(l -> d.getBizNo().equals(l.getDisposalBizNo())
                        && l.getDirection() == PointsDirection.MAKEUP && l.getPoints() == 30));
    }

    @Test
    @DisplayName("场景8：摄像头误判申诉不成立（整改不合格）→ 暂扣积分没收")
    void appealFailKeepsPenalty() {
        int before = residentRepo.findByCardNo("R1002").orElseThrow().getPointsBalance();
        DisposalRecord d = disposalService.create("R1002", "BR-002", WasteType.KITCHEN,
                AccessMethod.CARD, 1.0, AiVerdict.MIS_SORT, false);
        Rectification appeal = rectificationService.appeal(d.getBizNo(), "认为自己没投错");
        rectificationService.review(appeal.getRectifyNo(), RectifyResult.FAIL,
                "复核确认塑料袋混入厨余", "物业管家-小陈");
        DisposalRecord dAfter = disposalRepo.findById(d.getId()).orElseThrow();
        assertTrue(dAfter.getPointsSettled());
        assertFalse(dAfter.getPointsCredited());
        assertEquals(before - 20, residentRepo.findByCardNo("R1002").orElseThrow().getPointsBalance());
    }

    @Test
    @DisplayName("场景9：清运优先级随容量/异味/投诉/升级变化，环卫端按分数降序")
    void pickupPriorityReflectsRoomState() {
        deviceIngest.report("BR-002", DeviceEventType.CAPACITY, 40.0, "OTHER", null);
        deviceIngest.report("BR-001", DeviceEventType.CAPACITY, 96.0, "KITCHEN", null);
        deviceIngest.report("BR-001", DeviceEventType.ODOR, 80.0, null, null);
        complaintService.create(null, "BR-001", ComplaintType.OVERFLOW, "满溢扰民投诉");

        int high = roomStateService.pickupPriorityScore(
                roomRepo.findByCode("BR-001").orElseThrow(), WasteType.KITCHEN);
        int low = roomStateService.pickupPriorityScore(
                roomRepo.findByCode("BR-002").orElseThrow(), WasteType.OTHER);
        assertTrue(high > low, "满溢+异味+投诉的垃圾房优先级应更高: " + high + " vs " + low);

        taskService.recalcPriorities();
        List<PickupTask> open = pickupRepo.findByStatusIn(List.of(
                TaskStatus.PENDING, TaskStatus.DISPATCHED, TaskStatus.IN_PROGRESS,
                TaskStatus.OVERDUE, TaskStatus.ESCALATED));
        PickupTask top = open.stream().max((a, b) -> a.getPriorityScore() - b.getPriorityScore()).orElseThrow();
        assertEquals(br001.getId(), top.getBinRoom().getId());
    }

    @Test
    @DisplayName("街道端：分类质量按小区统计且投诉趋势可查")
    void streetStatsAggregateByCommunity() {
        disposalService.create("R1001", "BR-001", WasteType.KITCHEN,
                AccessMethod.CARD, 1.0, AiVerdict.CORRECT, false);
        disposalService.create("R1001", "BR-001", WasteType.KITCHEN,
                AccessMethod.CARD, 0.5, AiVerdict.MIS_SORT, false);
        complaintService.create(null, "BR-001", ComplaintType.ODOR, "统计用异味投诉");

        var quality = statsService.classificationQuality(7);
        var yg = quality.stream().filter(q -> "阳光花园小区".equals(q.get("communityName"))).findFirst();
        assertTrue(yg.isPresent());
        assertTrue((int) yg.get().get("misSort") >= 1);
        assertTrue((double) yg.get().get("correctRatePct") < 100.0);

        var trend = statsService.complaintTrend(14);
        assertTrue((int) trend.get("totalComplaints") >= 1);
        assertEquals(14, ((List<?>) trend.get("dates")).size());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
