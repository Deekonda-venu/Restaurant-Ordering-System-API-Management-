package com.example.Payment_Service.Event;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentEvent {
    private String eventType;       // PAYMENT_SUCCESS or PAYMENT_FAILED
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String status;          // SUCCESS or FAILED
    private String transactionId;
    private LocalDateTime occurredAt;
}