package com.city.binroom.web;

import com.city.binroom.domain.enums.DeviceEventType;
import com.city.binroom.service.DeviceIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 设备数据上报入口（模拟垃圾房设备网关，免鉴权便于设备侧接入演示）：
 * 厨余桶满溢 / 容量 / 异味 / 门禁 / 摄像头识别 / 称重 / 消杀 / 清运车到场 / 心跳
 */
@Tag(name = "设备接入")
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceIngestService ingestService;

    public DeviceController(DeviceIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @Operation(summary = "设备事件上报")
    @PostMapping("/events")
    public Map<String, Object> report(@RequestBody DeviceEventRequest req) {
        Long eventId = ingestService.report(req.getRoomCode(), req.getEventType(),
                req.getNumericValue(), req.getWasteType(), req.getPayload());
        return Map.of("eventId", eventId, "accepted", true);
    }

    @Data
    public static class DeviceEventRequest {
        @NotBlank
        private String roomCode;
        @NotNull
        private DeviceEventType eventType;
        /** 容量百分比 / 异味值 / 重量等 */
        private Double numericValue;
        /** KITCHEN / OTHER（容量上报时区分桶型） */
        private String wasteType;
        /** 附加 JSON 字符串，如 {"arrived":true}、COMPLETE */
        private String payload;
    }
}
