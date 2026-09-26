package com.example.Kitchen_Serivce.Listener;

import com.example.Kitchen_Serivce.Event.OrderPlacedEvent;
import com.example.Kitchen_Serivce.Model.KitchenOrder;
import com.example.Kitchen_Serivce.Repo.KitchenOrderRepo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderEventConsumer {

    private final KitchenOrderRepo kitchenOrderRepo;

    public OrderEventConsumer(KitchenOrderRepo kitchenOrderRepo) {
        this.kitchenOrderRepo = kitchenOrderRepo;
    }

    @KafkaListener(topics = "order-events", groupId = "kitchen-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Ignore unrelated events or broken messages.
        // The kitchen service only cares about ORDER_PLACED events.
        if (!"ORDER_PLACED".equals(event.getEventType()) || event.getOrderId() == null) {
            return;
        }

        // Prevent duplicate kitchen records for the same order.
        // This avoids creating more than one kitchen ticket per order.
        if (kitchenOrderRepo.findByOrderId(event.getOrderId()).isPresent()) {
            return;
        }

        // Create a kitchen order record in MongoDB.
        // This acts like the kitchen's internal queue for the new order.
        KitchenOrder ticket = new KitchenOrder();
        ticket.setOrderId(event.getOrderId());
        ticket.setRestaurantId(event.getRestaurantId());
        ticket.setStatus("RECEIVED");
        ticket.setReceivedAt(LocalDateTime.now());
        kitchenOrderRepo.save(ticket);
        System.out.println("KITCHEN: created RECEIVED ticket for order " + event.getOrderId());
    }
}