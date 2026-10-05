package com.city.garbage.event;

import java.time.OffsetDateTime;

/**
 * 垃圾房设备统一上报事件（设备 -> 平台，Kafka topic: device-events）。
 * 不同 eventType 使用不同字段，未用字段允许为空。
 *
 * @param eventType      事件类型
 * @param roomCode       垃圾房编码
 * @param deviceCode     设备编码（摄像头/异味传感器/门禁/地磅/消杀设备）
 * @param binType        桶类型 KITCHEN/OTHER/RECYCLABLE/HAZARDOUS（满溢、容量、称重类事件使用）
 * @param capacityPct    容量百分比 0-100（满溢/容量事件）
 * @param odorValue      异味值 0-100（异味事件）
 * @param weightKg       重量（kg，称重事件）
 * @param cardNo         刷卡卡号/扫码用户标识（门禁、摄像头、称重事件）
 * @param detectedType   摄像头识别出的垃圾类型
 * @param confidence     摄像头置信度 0-1
 * @param violationCode  识别到的违规编码，如 MISSORT（错投）、MIXED_BAG（混装袋装）
 * @param opened         门禁是否打开（门禁事件）
 * @param disinfected    消杀设备是否完成一次消杀
 * @param truckPresent   清运车是否在场
 * @param occurredAt     设备侧事件发生时间
 */
public record DeviceEvent(
        DeviceEventType eventType,
        String roomCode,
        String deviceCode,
        String binType,
        Integer capacityPct,
        Integer odorValue,
        Double weightKg,
        String cardNo,
        String detectedType,
        Double confidence,
        String violationCode,
        Boolean opened,
        Boolean disinfected,
        Boolean truckPresent,
        OffsetDateTime occurredAt
) {
    public static DeviceEventBuilder builder() {
        return new DeviceEventBuilder();
    }

    public static final class DeviceEventBuilder {
        private DeviceEventType eventType;
        private String roomCode;
        private String deviceCode;
        private String binType;
        private Integer capacityPct;
        private Integer odorValue;
        private Double weightKg;
        private String cardNo;
        private String detectedType;
        private Double confidence;
        private String violationCode;
        private Boolean opened;
        private Boolean disinfected;
        private Boolean truckPresent;
        private OffsetDateTime occurredAt;

        public DeviceEventBuilder eventType(DeviceEventType v) { this.eventType = v; return this; }
        public DeviceEventBuilder roomCode(String v) { this.roomCode = v; return this; }
        public DeviceEventBuilder deviceCode(String v) { this.deviceCode = v; return this; }
        public DeviceEventBuilder binType(String v) { this.binType = v; return this; }
        public DeviceEventBuilder capacityPct(Integer v) { this.capacityPct = v; return this; }
        public DeviceEventBuilder odorValue(Integer v) { this.odorValue = v; return this; }
        public DeviceEventBuilder weightKg(Double v) { this.weightKg = v; return this; }
        public DeviceEventBuilder cardNo(String v) { this.cardNo = v; return this; }
        public DeviceEventBuilder detectedType(String v) { this.detectedType = v; return this; }
        public DeviceEventBuilder confidence(Double v) { this.confidence = v; return this; }
        public DeviceEventBuilder violationCode(String v) { this.violationCode = v; return this; }
        public DeviceEventBuilder opened(Boolean v) { this.opened = v; return this; }
        public DeviceEventBuilder disinfected(Boolean v) { this.disinfected = v; return this; }
        public DeviceEventBuilder truckPresent(Boolean v) { this.truckPresent = v; return this; }
        public DeviceEventBuilder occurredAt(OffsetDateTime v) { this.occurredAt = v; return this; }

        public DeviceEvent build() {
            return new DeviceEvent(eventType, roomCode, deviceCode, binType, capacityPct, odorValue,
                    weightKg, cardNo, detectedType, confidence, violationCode, opened,
                    disinfected, truckPresent, occurredAt);
        }
    }

    /* bean 风格访问器，便于 Jackson 之外的框架与既有调用点使用 */
    public DeviceEventType getEventType() { return eventType; }
    public String getRoomCode() { return roomCode; }
    public String getDeviceCode() { return deviceCode; }
    public String getBinType() { return binType; }
    public Integer getCapacityPct() { return capacityPct; }
    public Integer getOdorValue() { return odorValue; }
    public Double getWeightKg() { return weightKg; }
    public String getCardNo() { return cardNo; }
    public String getDetectedType() { return detectedType; }
    public Double getConfidence() { return confidence; }
    public String getViolationCode() { return violationCode; }
    public Boolean getOpened() { return opened; }
    public Boolean getDisinfected() { return disinfected; }
    public Boolean getTruckPresent() { return truckPresent; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
