package com.example.Kitchen_Serivce.Service;

import com.example.Kitchen_Serivce.Event.KitchenEvent;
import com.example.Kitchen_Serivce.Event.KitchenEventPublisher;
import com.example.Kitchen_Serivce.Model.KitchenOrder;
import com.example.Kitchen_Serivce.Repo.KitchenOrderRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KitchenService {

    private final KitchenOrderRepo repo;
    private final KitchenEventPublisher publisher;

    public KitchenService(KitchenOrderRepo repo, KitchenEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    public KitchenOrder updateStatus(Long orderId, String status) {
        // Accept only valid kitchen states.
        // PREPARING and READY are the two workflow states for the kitchen service.
        if (status == null || !("PREPARING".equalsIgnoreCase(status)
                || "READY".equalsIgnoreCase(status))) {
            throw new IllegalArgumentException("Status must be PREPARING or READY");
        }

        // Find the kitchen ticket for that order.
        // The kitchen works from MongoDB, not from the main order database.
        KitchenOrder order = repo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Kitchen order not found: " + orderId));
        String normalizedStatus = status.toUpperCase();
        order.setStatus(normalizedStatus);

        // Record when the cooking started or when the food was completed.
        if ("PREPARING".equals(normalizedStatus)) {
            order.setStartedAt(LocalDateTime.now());
        } else {
            order.setReadyAt(LocalDateTime.now());
        }
        KitchenOrder saved = repo.save(order);

        // Publish a kitchen event to Kafka.
        // Delivery or Notification services may react to ORDER_PREPARING and ORDER_READY.
        KitchenEvent event = new KitchenEvent();
        event.setOrderId(saved.getOrderId());
        event.setRestaurantId(saved.getRestaurantId());
        event.setEventType("PREPARING".equals(normalizedStatus)
                ? "ORDER_PREPARING" : "ORDER_READY");
        publisher.publish(event);
        return saved;
    }
}