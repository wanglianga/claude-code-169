package com.city.binroom.service.processor;

import com.city.binroom.config.LinkageProperties;
import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.enums.LinkageType;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.event.EventTypes;
import com.city.binroom.event.LinkageMessage;
import com.city.binroom.event.LinkageProcessor;
import com.city.binroom.service.GovernanceTaskService;
import com.city.binroom.service.RoomStateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备事件联动：
 * - 厨余桶满溢：派环卫清运（满溢但车未到，超时再升级）
 * - 异味越限：派物业消杀
 * - 满溢+异味同时存在：清运与消杀同批派单，避免只治一个告警
 * - 夜间投放高峰：物业巡查 + 环卫优先关注
 * - 清运车到场：解除“满但车未到”告警
 */
@Component
@RequiredArgsConstructor
public class DeviceLinkageProcessor implements LinkageProcessor {

    private final BinRoomRepository roomRepo;
    private final GovernanceTaskService taskService;
    private final RoomStateService stateService;
    private final LinkageProperties props;

    @Override
    public boolean supports(LinkageMessage m) {
        return EventTypes.DEVICE_REPORTED.equals(m.getType());
    }

    @Override
    @Transactional
    public void handle(LinkageMessage m) {
        JsonNode p = m.getPayload();
        Long roomId = p.get("roomId").asLong();
        BinRoom room = roomRepo.findById(roomId).orElseThrow();
        String eventType = p.get("eventType").asText();
        boolean crossing = p.path("crossing").asBoolean(false);

        boolean full = room.getKitchenFillPct() >= props.getFullThresholdPct();
        boolean otherFull = room.getOtherFillPct() >= props.getFullThresholdPct();
        boolean odor = room.getOdorLevel() >= props.getOdorThreshold();

        switch (eventType) {
            case "KITCHEN_FULL", "CAPACITY" -> {
                if (full) {
                    PickupTask pickup = taskService.createPickupIfAbsent(room, WasteType.KITCHEN,
                            "厨余桶容量 " + room.getKitchenFillPct() + "% 达到满溢阈值，清运车"
                                    + (Boolean.TRUE.equals(room.getTruckOnSite()) ? "已到场" : "尚未到场"),
                            LinkageType.FULL_NO_TRUCK.name(), props.getPickupTimeoutMinutes());
                    if (crossing) {
                        stateService.log(room, LinkageType.FULL_NO_TRUCK.name(), "TRIGGER",
                                "厨余桶满溢 " + room.getKitchenFillPct() + "%，清运车"
                                        + (Boolean.TRUE.equals(room.getTruckOnSite()) ? "已在现场" : "未到，已生成清运任务 " + pickup.getTaskNo()),
                                pickup.getTaskNo());
                    }
                }
                if (otherFull) {
                    PickupTask pickup = taskService.createPickupIfAbsent(room, WasteType.OTHER,
                            "其他垃圾桶容量 " + room.getOtherFillPct() + "% 达到满溢阈值",
                            LinkageType.FULL_NO_TRUCK.name(), props.getPickupTimeoutMinutes());
                    if (crossing) {
                        stateService.log(room, LinkageType.FULL_NO_TRUCK.name(), "TRIGGER",
                                "其他垃圾桶满溢 " + room.getOtherFillPct() + "%，已生成清运任务 " + pickup.getTaskNo(),
                                pickup.getTaskNo());
                    }
                }
            }
            case "ODOR" -> {
                if (odor) {
                    CleaningTask disinfect = taskService.createCleaningIfAbsent(room, "DISINFECT",
                            "异味传感器 " + room.getOdorLevel() + " 超过阈值 " + props.getOdorThreshold()
                                    + "，需消杀除臭" + (full ? "；当前同时满溢，已同步安排清运" : ""),
                            LinkageType.ODOR_ALERT.name(), props.getCleaningTimeoutMinutes());
                    if (crossing) {
                        stateService.log(room, LinkageType.ODOR_ALERT.name(), "TRIGGER",
                                "异味越限 " + room.getOdorLevel() + "，已派消杀任务 " + disinfect.getTaskNo(),
                                disinfect.getTaskNo());
                    }
                }
            }
            default -> { /* 其他设备事件无需本处理器动作 */ }
        }

        // 满溢与异味并发时补齐另一侧任务：两类告警同批治理
        if (full && odor) {
            PickupTask pickup = taskService.createPickupIfAbsent(room, WasteType.KITCHEN,
                    "满溢+异味并发，厨余优先清运，异味 " + room.getOdorLevel(),
                    LinkageType.FULL_NO_TRUCK.name(), props.getPickupTimeoutMinutes());
            CleaningTask disinfect = taskService.createCleaningIfAbsent(room, "DISINFECT",
                    "满溢+异味并发，同步消杀，厨余容量 " + room.getKitchenFillPct() + "%",
                    LinkageType.ODOR_ALERT.name(), props.getCleaningTimeoutMinutes());
            if (crossing) {
                stateService.log(room, LinkageType.FULL_NO_TRUCK.name(), "TRIGGER",
                        "满溢与异味同时出现，联动派单：清运 " + pickup.getTaskNo() + " + 消杀 " + disinfect.getTaskNo(),
                        pickup.getTaskNo());
            }
        }

        // 清运车到场事件：记录告警解除
        if ("TRUCK_ARRIVAL".equals(eventType) && Boolean.TRUE.equals(room.getTruckOnSite())) {
            stateService.log(room, LinkageType.FULL_NO_TRUCK.name(), "COMPLETE",
                    "清运车已到场，满溢等待解除", null);
        }
    }
}
