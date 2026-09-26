package com.example.Delivary_Serivce.Service;

import com.example.Delivary_Serivce.Event.DeliveryEvent;
import com.example.Delivary_Serivce.Event.DeliveryEventPublisher;
import com.example.Delivary_Serivce.Model.Delivery;
import com.example.Delivary_Serivce.Repo.DeliveryRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeliveryService {
    private final DeliveryRepo deliveryRepo;
    private final DeliveryEventPublisher publisher;

    public DeliveryService(DeliveryRepo deliveryRepo, DeliveryEventPublisher publisher) {
        this.deliveryRepo = deliveryRepo;
        this.publisher = publisher;
    }

    public Delivery assignDriver(Long orderId, Long driverId) {
        // This method assigns a delivery driver to an order that is already ready for pickup.
        Delivery delivery = getByOrderId(orderId);
        delivery.setDriverId(driverId);
        return deliveryRepo.save(delivery);
    }

    public Delivery updateStatus(Long orderId, String status) {
        // Only valid delivery states are allowed to keep the lifecycle consistent.
        if (status == null || !("PICKED_UP".equalsIgnoreCase(status)
                || "ON_THE_WAY".equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status))) {
            throw new IllegalArgumentException("Status must be PICKED_UP, ON_THE_WAY or DELIVERED");
        }

        Delivery delivery = getByOrderId(orderId);
        delivery.setStatus(status.toUpperCase());

        if ("PICKED_UP".equalsIgnoreCase(status)) {
            delivery.setPickupTime(LocalDateTime.now());
        }
        if ("DELIVERED".equalsIgnoreCase(status)) {
            delivery.setDeliveryTime(LocalDateTime.now());
        }

        Delivery saved = deliveryRepo.save(delivery);

        // Publish a delivery event so Notification can send the customer updates.
        DeliveryEvent event = new DeliveryEvent();
        event.setOrderId(saved.getOrderId());
        event.setDriverId(saved.getDriverId());
        event.setCustomerId(saved.getCustomerId());
        event.setEventType(switch (status.toUpperCase()) {
            case "PICKED_UP" -> "ORDER_PICKED_UP";
            case "ON_THE_WAY" -> "ORDER_ON_THE_WAY";
            case "DELIVERED" -> "ORDER_DELIVERED";
            default -> "DELIVERY_UPDATE";
        });
        publisher.publish(event);

        return saved;
    }

    private Delivery getByOrderId(Long orderId) {
        return deliveryRepo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));
    }
}
