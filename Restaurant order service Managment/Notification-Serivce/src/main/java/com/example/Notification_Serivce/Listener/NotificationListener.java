package com.example.Notification_Serivce.Listener;

import com.example.Notification_Serivce.Event.GenericEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @KafkaListener(
            topics = {
                    "order-events",
                    "payment-events",
                    "kitchen-events",
                    "delivery-events"
            },
            groupId = "notification-service"
    )
    public void onEvent(GenericEvent event) {
        // This listener listens to all important events from the platform.
        // The goal is to convert each event into a user-facing notification message.
        String message = switch (
                event.getEventType() == null
                        ? ""
                        : event.getEventType()
                ) {
            case "ORDER_PLACED" ->
                    "Your order #" + event.getOrderId()
                            + " has been placed.";

            case "PAYMENT_SUCCESS" ->
                    "Payment successful for order #"
                            + event.getOrderId() + ".";

            case "PAYMENT_FAILED" ->
                    "Payment failed for order #"
                            + event.getOrderId() + ".";

            case "ORDER_PREPARING" ->
                    "Your food for order #" + event.getOrderId()
                            + " is being prepared.";

            case "ORDER_READY" ->
                    "Your food for order #" + event.getOrderId()
                            + " is ready.";

            case "ORDER_PICKED_UP" ->
                    "Your order #" + event.getOrderId()
                            + " was picked up.";

            case "ORDER_ON_THE_WAY" ->
                    "Your order #" + event.getOrderId()
                            + " is on the way.";

            case "ORDER_DELIVERED" ->
                    "Your order #" + event.getOrderId()
                            + " was delivered.";

            default ->
                    "Update for order #" + event.getOrderId();
        };

        System.out.println(
                "NOTIFICATION: customer=" + event.getCustomerId()
                        + ", order=" + event.getOrderId()
                        + ", message=" + message
        );
    }
}