package com.example.Delivary_Serivce.Listener;

import com.example.Delivary_Serivce.Event.KitchenEvent;
import com.example.Delivary_Serivce.Model.Delivery;
import com.example.Delivary_Serivce.Repo.DeliveryRepo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class KitchenEventConsumer {
    private final DeliveryRepo deliveryRepo;

    public KitchenEventConsumer(DeliveryRepo deliveryRepo) {
        this.deliveryRepo = deliveryRepo;
    }

    @KafkaListener(topics = "kitchen-events", groupId = "delivery-service")
    public void onKitchenEvent(KitchenEvent event) {
        // Ignore unrelated kitchen updates, because delivery only starts when the food is ready.
        if (!"ORDER_READY".equals(event.getEventType())) {
            return;
        }

        // Prevent duplicate delivery tasks for the same order.
        if (deliveryRepo.findByOrderId(event.getOrderId()).isPresent()) {
            return;
        }

        // Create a new delivery document once the kitchen says the order is ready.
        Delivery delivery = new Delivery();
        delivery.setOrderId(event.getOrderId());
        delivery.setRestaurantId(event.getRestaurantId());
        delivery.setCustomerId(event.getCustomerId());
        delivery.setStatus("READY_FOR_PICKUP");
        delivery.setCreatedAt(LocalDateTime.now());
        deliveryRepo.save(delivery);

        System.out.println("DELIVERY: created READY_FOR_PICKUP for order " + event.getOrderId());
    }
}
