package com.example.Order_Service.Event;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPlacedEvent {
    // Unique id for this Kafka event.
    private String eventId;

    // Event name sent to downstream services such as Kitchen and Notification.
    private String eventType;

    // The order created in the Order service.
    private Long orderId;

    // Customer who placed the order.
    private Long customerId;

    // Restaurant that will prepare the order.
    private Long restaurantId;

    // Final amount for the order.
    private BigDecimal totalAmount;

    // When the order was created.
    private LocalDateTime createdAt;
}