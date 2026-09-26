package com.example.Kitchen_Serivce.Event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPlacedEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}