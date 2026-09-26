package com.example.Delivary_Serivce.Event;

import lombok.Data;

@Data
public class DeliveryEvent {
    // This event is published by the delivery service to notify other services.
    // It tells the system how the delivery status changed.
    private String eventType; // ORDER_PICKED_UP | ORDER_ON_THE_WAY | ORDER_DELIVERED
    private Long orderId;
    private Long driverId;
    private Long customerId;
}
