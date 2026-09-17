package com.example.MenuService.Response;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ResturantFullDetailsResponse {
    private Long id;
    private String resturantName;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String city;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
