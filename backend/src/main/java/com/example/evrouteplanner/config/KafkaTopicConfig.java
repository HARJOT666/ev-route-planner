package com.example.evrouteplanner.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topic used for charging station status changes.
 * Spring creates the topic automatically on startup if it does not exist.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String STATION_EVENTS_TOPIC = "charging-station-events";

    @Bean
    public NewTopic chargingStationEventsTopic() {
        return TopicBuilder.name(STATION_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
