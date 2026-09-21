package com.example.Payment_Service.Event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "payment-events";

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(
                TOPIC,
                String.valueOf(event.getOrderId()),
                event
        );
    }
}