package com.example.Notification_Serivce.Event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericEvent {
    private String eventType;
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private String status;
}