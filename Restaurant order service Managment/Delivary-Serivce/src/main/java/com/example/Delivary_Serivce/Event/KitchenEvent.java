package com.example.Delivary_Serivce.Event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KitchenEvent {
    // This event comes from the Kitchen service when the food is ready to be delivered.
    // We only care about ORDER_READY messages for the delivery workflow.
    private String eventType;
    private Long orderId;
    private Long restaurantId;
    private Long customerId;
}
