package com.city.binroom.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** kafka profile 下自动创建联动主题 */
@Configuration
@Profile("kafka")
public class KafkaTopicConfig {

    @Bean
    public NewTopic deviceEventsTopic() {
        return new NewTopic(Topics.DEVICE_EVENTS, 3, (short) 1);
    }

    @Bean
    public NewTopic pointsEventsTopic() {
        return new NewTopic(Topics.POINTS_EVENTS, 2, (short) 1);
    }

    @Bean
    public NewTopic linkageEventsTopic() {
        return new NewTopic(Topics.LINKAGE_EVENTS, 3, (short) 1);
    }
}
