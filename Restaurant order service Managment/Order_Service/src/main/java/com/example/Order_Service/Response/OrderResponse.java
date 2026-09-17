package com.example.Order_Service.Response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonPropertyOrder({"orderId", "customerDetails", "restaurantDetails", "items", "status", "subtotal", "tax", "deliveryFee", "totalAmount", "paymentStatus", "createdAt", "updatedAt"})
public class OrderResponse {

    private Long orderId;
    private CustomerResponse customerDetails;
    private ResturantResponse restaurantDetails;
    private List<MenuitemsResponse> items;       // class is spelled "MenuitemsResponse"
    private String status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
