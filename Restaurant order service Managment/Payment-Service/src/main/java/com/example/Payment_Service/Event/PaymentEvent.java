package com.example.Payment_Service.Event;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentEvent {
    // Kafka event type: PAYMENT_SUCCESS or PAYMENT_FAILED.
    private String eventType;

    // Payment record id saved in the Payment database.
    private Long paymentId;

    // Order for which payment was created.
    private Long orderId;

    // Customer who paid for the order.
    private Long customerId;

    // Payment amount.
    private BigDecimal amount;

    // Final status: SUCCESS or FAILED.
    private String status;

    // Unique bank/transaction reference.
    private String transactionId;

    // When the payment event happened.
    private LocalDateTime occurredAt;
}