package com.city.binroom.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 跨服务联动消息。local 模式下进程内传递，kafka 模式下序列化为 JSON。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkageMessage {
    private String eventId;
    private String type;
    private JsonNode payload;
    private LocalDateTime occurredAt;
}
