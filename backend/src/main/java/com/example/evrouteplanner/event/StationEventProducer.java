package com.example.evrouteplanner.event;

import com.example.evrouteplanner.config.KafkaTopicConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes station status events to Kafka. The event is turned into a JSON
 * string and sent to the "charging-station-events" topic.
 */
@Component
public class StationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StationEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(StationStatusEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopicConfig.STATION_EVENTS_TOPIC, json);
            System.out.println("[Kafka] Published station event: " + json);
        } catch (Exception e) {
            // Publishing must never break the main request, so we just log it.
            System.out.println("[Kafka] Failed to publish station event: " + e.getMessage());
        }
    }
}
