package com.city.garbage.api;

import com.city.garbage.event.DeviceEvent;
import com.city.garbage.kafka.DeviceEventGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 设备端：垃圾房各类设备统一数据上报入口（Kafka topic device-events） */
@Tag(name = "设备端", description = "满溢/容量/异味/门禁/摄像头/称重/消杀/清运车/心跳上报")
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceEventGateway gateway;

    @Operation(summary = "设备事件上报",
            description = "支持 KITCHEN_FULL/OTHER_CAPACITY/ODOR/ACCESS/CAMERA_DETECTION/WEIGHT/"
                    + "DISINFECTION/TRUCK_ARRIVAL/HEARTBEAT；启用 Kafka 时异步入库联动，否则同步处理")
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> report(@Valid @RequestBody DeviceEvent event) {
        gateway.publish(event);
        return ResponseEntity.accepted().body(Map.of(
                "accepted", true,
                "roomCode", event.getRoomCode(),
                "eventType", event.getEventType()));
    }
}
