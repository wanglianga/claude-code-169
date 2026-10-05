package com.city.binroom.service;

import com.city.binroom.domain.entity.BinRoom;
import com.city.binroom.domain.entity.DeviceEvent;
import com.city.binroom.domain.enums.DeviceEventType;
import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.DeviceEventRepository;
import com.city.binroom.config.LinkageProperties;
import com.city.binroom.event.EventPublisher;
import com.city.binroom.event.EventTypes;
import com.city.binroom.event.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 设备数据接入：落原始事件、更新垃圾房聚合状态、事务提交后发设备联动事件 */
@Service
@RequiredArgsConstructor
public class DeviceIngestService {

    private final BinRoomRepository roomRepo;
    private final DeviceEventRepository eventRepo;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final LinkageProperties props;

    @Transactional
    public Long report(String roomCode, DeviceEventType type, Double numericValue,
                       String wasteType, String payloadJson) {
        BinRoom room = roomRepo.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("垃圾房不存在: " + roomCode));
        LocalDateTime now = LocalDateTime.now();

        DeviceEvent event = new DeviceEvent();
        event.setBinRoom(room);
        event.setEventType(type);
        event.setNumericValue(numericValue);
        event.setWasteType(wasteType);
        event.setPayload(payloadJson);
        event.setEventTime(now);
        event.setReceivedAt(now);
        eventRepo.save(event);

        // 任何上报都代表设备在线
        room.setOnline(true);
        room.setLastHeartbeatAt(now);

        boolean crossing = applyState(room, type, numericValue, wasteType, payloadJson, now);
        room.setUpdatedAt(now);
        roomRepo.save(room);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("roomId", room.getId());
        payload.put("eventDbId", event.getId());
        payload.put("eventType", type.name());
        payload.put("crossing", crossing);
        if (numericValue != null) {
            payload.put("numericValue", numericValue);
        }
        if (wasteType != null) {
            payload.put("wasteType", wasteType);
        }
        if (payloadJson != null) {
            payload.put("rawPayload", payloadJson);
        }
        eventPublisher.publish(Topics.DEVICE_EVENTS, EventTypes.DEVICE_REPORTED, payload);
        return event.getId();
    }

    /** 返回是否发生了“穿越阈值”等关键状态变化 */
    private boolean applyState(BinRoom room, DeviceEventType type, Double value,
                               String wasteType, String payloadJson, LocalDateTime now) {
        boolean crossing = false;
        switch (type) {
            case HEARTBEAT, ACCESS, AI_RECOGNITION, WEIGH -> { /* 状态无需更新 */ }
            case CAPACITY -> {
                int pct = value == null ? room.getKitchenFillPct() : (int) Math.round(value);
                if ("KITCHEN".equalsIgnoreCase(wasteType)) {
                    crossing = room.getKitchenFillPct() < props.getFullThresholdPct() && pct >= props.getFullThresholdPct();
                    room.setKitchenFillPct(pct);
                    if (pct >= props.getFullThresholdPct() && room.getKitchenFullSince() == null) {
                        room.setKitchenFullSince(now);
                    }
                    if (pct < props.getFullThresholdPct()) {
                        room.setKitchenFullSince(null);
                    }
                } else if ("OTHER".equalsIgnoreCase(wasteType)) {
                    crossing = room.getOtherFillPct() < props.getFullThresholdPct() && pct >= props.getFullThresholdPct();
                    room.setOtherFillPct(pct);
                }
            }
            case KITCHEN_FULL -> {
                int pct = value != null ? (int) Math.round(value) : props.getFullThresholdPct() + 5;
                crossing = room.getKitchenFillPct() < props.getFullThresholdPct();
                room.setKitchenFillPct(Math.max(room.getKitchenFillPct(), pct));
                if (room.getKitchenFullSince() == null) {
                    room.setKitchenFullSince(now);
                }
            }
            case ODOR -> {
                int level = value == null ? room.getOdorLevel() : (int) Math.round(value);
                crossing = room.getOdorLevel() < props.getOdorThreshold() && level >= props.getOdorThreshold();
                room.setOdorLevel(level);
            }
            case DISINFECT -> {
                if (payloadJson != null && payloadJson.contains("COMPLETE")) {
                    room.setLastCleanedAt(now);
                    room.setOdorLevel(Math.min(room.getOdorLevel(), 10));
                }
            }
            case TRUCK_ARRIVAL -> {
                boolean arrived = payloadJson == null || !payloadJson.contains("\"arrived\":false");
                room.setTruckOnSite(arrived);
            }
        }
        return crossing;
    }
}
