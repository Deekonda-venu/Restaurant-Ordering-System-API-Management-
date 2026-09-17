package com.example.Resturant.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "resturant_details")
@Data
@JsonPropertyOrder({"id", "resturantName", "description", "phone", "email", "address", "city", "openingTime", "closingTime", "status", "createdAt", "updatedAt"})
public class ResturantDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String resturantName;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String city;
    private LocalTime openingTime;
    private LocalTime closingTime;
    @Enumerated(EnumType.STRING)
    private ResturantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
