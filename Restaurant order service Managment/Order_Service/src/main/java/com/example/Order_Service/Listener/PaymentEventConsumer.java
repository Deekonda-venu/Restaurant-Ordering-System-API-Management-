package com.example.Order_Service.Listener;

import com.example.Order_Service.Event.PaymentEvent;
import com.example.Order_Service.Model.OrderDetails;
import com.example.Order_Service.Repositary.OrderDetailesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PaymentEventConsumer {

    @Autowired
    private OrderDetailesRepo orderDetailesRepo;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void onPaymentEvent(PaymentEvent event) {
        OrderDetails order = orderDetailesRepo.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            System.out.println("ORDER: no order found for id " + event.getOrderId());
            return;
        }
        if ("PAYMENT_SUCCESS".equals(event.getEventType())) {
            order.setPaymentStatus("SUCCESS");
        } else {
            order.setPaymentStatus("FAILED");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderDetailesRepo.save(order);
        System.out.println("ORDER: order " + order.getId() + " paymentStatus -> " + order.getPaymentStatus());
    }
}