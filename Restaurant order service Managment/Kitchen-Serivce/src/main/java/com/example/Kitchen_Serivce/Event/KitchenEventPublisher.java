package com.example.Kitchen_Serivce.Event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KitchenEventPublisher {

    private static final String TOPIC = "kitchen-events";

    private final KafkaTemplate<String, KitchenEvent> kafkaTemplate;

    public KitchenEventPublisher(KafkaTemplate<String, KitchenEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(KitchenEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}