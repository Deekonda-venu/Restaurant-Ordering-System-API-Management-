package com.example.Kitchen_Serivce.Event;

import lombok.Data;

@Data
public class KitchenEvent {
    private String eventType;
    private Long orderId;
    private Long restaurantId;
}