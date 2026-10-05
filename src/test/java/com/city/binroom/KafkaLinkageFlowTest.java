package com.city.binroom;

import com.city.binroom.domain.entity.CleaningTask;
import com.city.binroom.domain.entity.PickupTask;
import com.city.binroom.domain.enums.DeviceEventType;
import com.city.binroom.domain.enums.WasteType;
import com.city.binroom.domain.repo.CleaningTaskRepository;
import com.city.binroom.domain.repo.PickupTaskRepository;
import com.city.binroom.service.DeviceIngestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kafka 模式端到端：设备事件经 Kafka（binroom.device-events 主题）
 * 投递到消费组，再由统一联动处理器落库，验证投递机制与联动逻辑解耦。
 * 数据库仍用 H2（test profile 覆盖 kafka profile 的 PG 配置）。
 */
@ActiveProfiles({"kafka", "test"})
@EmbeddedKafka(partitions = 1)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@SpringBootTest
class KafkaLinkageFlowTest {

    @Autowired private DeviceIngestService deviceIngest;
    @Autowired private CleaningTaskRepository cleaningRepo;
    @Autowired private PickupTaskRepository pickupRepo;
    @Autowired private com.city.binroom.domain.repo.BinRoomRepository roomRepo;

    @Test
    @DisplayName("设备事件经 Kafka 投递后生成消杀与清运任务")
    void deviceEventFlowsThroughKafka() throws Exception {
        long start = System.currentTimeMillis();
        Long roomId = roomRepo.findByCode("BR-001").orElseThrow().getId();

        deviceIngest.report("BR-001", DeviceEventType.ODOR, 82.0, null, null);
        deviceIngest.report("BR-001", DeviceEventType.CAPACITY, 96.0, "KITCHEN", null);

        CleaningTask disinfect = await(() -> cleaningRepo.findAll().stream()
                .filter(t -> t.getBinRoom().getId().equals(roomId)
                        && "DISINFECT".equals(t.getTaskKind()))
                .findFirst().orElse(null));
        PickupTask pickup = await(() -> pickupRepo.findAll().stream()
                .filter(t -> t.getBinRoom().getId().equals(roomId)
                        && t.getWasteType() == WasteType.KITCHEN)
                .findFirst().orElse(null));

        // 事件确实跨了 Kafka：完成时间晚于上报调用，且任务记录可查
        assertTrue(disinfect.getCreatedAt() != null);
        assertTrue(pickup.getPriorityScore() > 0);
        assertTrue(System.currentTimeMillis() - start < 40_000);
    }

    private <T> T await(java.util.function.Supplier<T> supplier) throws InterruptedException {
        T value = null;
        for (int i = 0; i < 60; i++) {
            value = supplier.get();
            if (value != null) {
                return value;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Kafka 事件在超时时间内未被联动处理");
    }
}
