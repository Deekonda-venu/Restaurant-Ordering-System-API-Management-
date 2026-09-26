package com.example.Delivary_Serivce.Event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher {
    private static final String TOPIC = "delivery-events";

    private final KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

    public DeliveryEventPublisher(KafkaTemplate<String, DeliveryEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DeliveryEvent event) {
        // We send the event with the orderId as key so all delivery updates for one order stay together.
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}
