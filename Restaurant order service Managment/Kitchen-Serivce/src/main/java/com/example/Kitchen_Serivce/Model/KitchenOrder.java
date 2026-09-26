package com.example.Kitchen_Serivce.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "kitchen_orders")
public class KitchenOrder {
    @Id
    private String id;
    @Indexed(unique = true)
    private Long orderId;
    private Long restaurantId;
    private String status;
    private LocalDateTime receivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime readyAt;
}