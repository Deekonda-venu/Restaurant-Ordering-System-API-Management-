package com.example.Delivary_Serivce.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "deliveries")
public class Delivery {
    // This document is the delivery task stored in MongoDB.
    // Each order gets one delivery record that follows the lifecycle from pickup to arrival.
    @Id
    private String id;

    @Indexed(unique = true)
    private Long orderId;

    private Long customerId;
    private Long restaurantId;
    private Long driverId;
    private String status; // WAITING_FOR_FOOD | READY_FOR_PICKUP | PICKED_UP | ON_THE_WAY | DELIVERED
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime createdAt;
}
