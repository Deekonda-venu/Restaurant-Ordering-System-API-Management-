package com.example.Payment_Service.Model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class paymentRequestBody {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
}
