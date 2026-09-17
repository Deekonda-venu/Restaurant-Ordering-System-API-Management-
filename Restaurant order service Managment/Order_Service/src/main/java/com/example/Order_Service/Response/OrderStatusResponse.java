package com.example.Order_Service.Response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusResponse {

    private Long orderId;
    private String status;
    private LocalDateTime updatedAt;
}
